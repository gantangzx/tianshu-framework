package com.gantang.tianshu.config;

import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Nacos 配置中心自动配置：引入本模块即具备从 Nacos 拉取配置、动态刷新的能力，无需任何注解。
 *
 * <p>启动期的拉取由 {@link ConfigEnvironmentPostProcessor} 完成；本配置复用同一个
 * {@link NacosConfigServiceManager}，为每个 dataId 注册监听，变更时替换对应属性源并发布
 * {@link EnvironmentChangeEvent}，由 spring-cloud-context 重绑 {@code @ConfigurationProperties}
 * / {@code @RefreshScope}。
 *
 * @author gantang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "tianshu.nacos.config", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(NacosConfigProperties.class)
public class ConfigAutoConfiguration {

    @Bean
    public NacosConfigRefresher nacosConfigRefresher(NacosConfigProperties properties,
                                                     ConfigurableEnvironment environment,
                                                     org.springframework.context.ApplicationContext context) {
        NacosConfigServiceManager manager = NacosConfigManagers.get();
        if (manager == null) {
            manager = new NacosConfigServiceManager(properties, environment);
            NacosConfigManagers.register(manager);
        }
        return new NacosConfigRefresher(properties, environment, context, manager);
    }

    /**
     * 注册监听并在变更时刷新属性源、发布环境变更事件。
     */
    static class NacosConfigRefresher {

        private static final Logger log = LoggerFactory.getLogger(NacosConfigRefresher.class);

        private final NacosConfigProperties properties;
        private final ConfigurableEnvironment environment;
        private final org.springframework.context.ApplicationContext context;
        private final NacosConfigServiceManager manager;

        NacosConfigRefresher(NacosConfigProperties properties,
                             ConfigurableEnvironment environment,
                             org.springframework.context.ApplicationContext context,
                             NacosConfigServiceManager manager) {
            this.properties = properties;
            this.environment = environment;
            this.context = context;
            this.manager = manager;
        }

        @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
        public void register() {
            if (!properties.isRefreshEnabled()) {
                return;
            }
            for (String dataId : dataIds()) {
                manager.addListener(dataId, properties.getGroup(), new AbstractSharedListener() {
                    @Override
                    public void innerReceive(String dataId, String group, String configInfo) {
                        handleChange(dataId, configInfo);
                    }
                });
                log.info("[tianshu-config] subscribed: dataId={}, group={}", dataId, properties.getGroup());
            }
        }

        private void handleChange(String dataId, String configInfo) {
            PropertySource<?> newSource = NacosConfigLoader.load(dataId, configInfo);
            if (newSource == null) {
                return;
            }
            Set<String> changedKeys = diff(dataId, newSource);
            MutablePropertySources sources = environment.getPropertySources();
            if (sources.contains(dataId)) {
                sources.replace(dataId, newSource);
            } else if (sources.contains("systemEnvironment")) {
                sources.addAfter("systemEnvironment", newSource);
            } else {
                sources.addFirst(newSource);
            }
            log.info("[tianshu-config] refreshed: dataId={}, changedKeys={}", dataId, changedKeys);
            context.publishEvent(new EnvironmentChangeEvent(changedKeys));
        }

        @SuppressWarnings("unchecked")
        private Set<String> diff(String dataId, PropertySource<?> newSource) {
            Set<String> keys = new LinkedHashSet<>();
            PropertySource<?> old = environment.getPropertySources().get(dataId);
            if (old instanceof org.springframework.core.env.EnumerablePropertySource
                    && newSource instanceof org.springframework.core.env.EnumerablePropertySource) {
                org.springframework.core.env.EnumerablePropertySource<?> o =
                        (org.springframework.core.env.EnumerablePropertySource<?>) old;
                org.springframework.core.env.EnumerablePropertySource<?> n =
                        (org.springframework.core.env.EnumerablePropertySource<?>) newSource;
                Set<String> names = new LinkedHashSet<>();
                for (String k : n.getPropertyNames()) {
                    names.add(k);
                }
                for (String k : o.getPropertyNames()) {
                    names.add(k);
                }
                for (String k : names) {
                    Object ov = o.getProperty(k);
                    Object nv = n.getProperty(k);
                    if (ov == null ? nv != null : !ov.equals(nv)) {
                        keys.add(k);
                    }
                }
            } else {
                if (newSource instanceof org.springframework.core.env.EnumerablePropertySource) {
                    for (String k : ((org.springframework.core.env.EnumerablePropertySource<?>) newSource)
                            .getPropertyNames()) {
                        keys.add(k);
                    }
                }
            }
            return keys;
        }

        private Set<String> dataIds() {
            Set<String> dataIds = new LinkedHashSet<>();
            String appName = environment.getProperty("spring.application.name", "application");
            dataIds.add(appName + "." + properties.getFileExtension());
            dataIds.addAll(properties.getSharedConfigs());
            return dataIds;
        }
    }
}
