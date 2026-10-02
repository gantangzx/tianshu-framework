package com.gantang.tianshu.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 启动早期从 Nacos 拉取配置并加入环境。
 *
 * <p>职责：
 * <ol>
 *     <li>以最低优先级载入 {@code config-defaults.yml}（开箱默认值）；</li>
 *     <li>若启用，则通过 {@link NacosConfigServiceManager} 拉取默认 dataId 与 shared-configs；</li>
 *     <li>把拉取到的属性源插入到 {@code systemEnvironment} 之后（存在则之后，否则最前），
 *     使其优先级高于本地 {@code application.yml}、但低于命令行与环境变量。</li>
 * </ol>
 *
 * <p>创建出的 {@link NacosConfigServiceManager} 放入静态注册表，供后续上下文复用同一个 ConfigService。
 *
 * @author gantang
 */
public class ConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConfigEnvironmentPostProcessor.class);
    private static final String DEFAULTS_RESOURCE = "config-defaults.yml";
    private static final String DEFAULTS_SOURCE = "tianshuConfigDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        loadDefaults(environment);
        if (!environment.getProperty("tianshu.nacos.config.enabled", Boolean.class, true)) {
            return;
        }

        NacosConfigProperties properties = bindProperties(environment);
        NacosConfigServiceManager manager = new NacosConfigServiceManager(properties, environment);
        NacosConfigManagers.register(manager);

        List<PropertySource<?>> remoteSources = fetchAll(environment, properties, manager);
        for (PropertySource<?> source : remoteSources) {
            insertAfterSystemEnvironment(environment, source);
            log.info("[tianshu-config] loaded config from Nacos: {}", source.getName());
        }
    }

    private void loadDefaults(ConfigurableEnvironment environment) {
        ClassPathResource resource = new ClassPathResource(DEFAULTS_RESOURCE);
        if (!resource.exists()) {
            return;
        }
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(DEFAULTS_SOURCE, resource);
            sources.forEach(source -> environment.getPropertySources().addLast(source));
        } catch (IOException e) {
            throw new IllegalStateException("加载 " + DEFAULTS_RESOURCE + " 失败", e);
        }
    }

    private NacosConfigProperties bindProperties(ConfigurableEnvironment environment) {
        NacosConfigProperties properties = new NacosConfigProperties();
        org.springframework.boot.context.properties.bind.Binder.get(environment)
                .bind("tianshu.nacos.config",
                        org.springframework.boot.context.properties.bind.Bindable.ofInstance(properties));
        return properties;
    }

    private List<PropertySource<?>> fetchAll(ConfigurableEnvironment environment,
                                             NacosConfigProperties properties,
                                             NacosConfigServiceManager manager) {
        Set<String> dataIds = new LinkedHashSet<>();
        String appName = environment.getProperty("spring.application.name", "application");
        dataIds.add(appName + "." + properties.getFileExtension());
        dataIds.addAll(properties.getSharedConfigs());

        List<PropertySource<?>> result = new ArrayList<>();
        for (String dataId : dataIds) {
            String content = manager.getConfig(dataId, properties.getGroup());
            PropertySource<?> source = NacosConfigLoader.load(dataId, content);
            if (source != null) {
                result.add(source);
            } else {
                log.debug("[tianshu-config] config not found or empty: dataId={}, group={}",
                        dataId, properties.getGroup());
            }
        }
        return result;
    }

    private void insertAfterSystemEnvironment(ConfigurableEnvironment environment, PropertySource<?> source) {
        if (environment.getPropertySources().contains(source.getName())) {
            environment.getPropertySources().replace(source.getName(), source);
            return;
        }
        if (environment.getPropertySources().contains("systemEnvironment")) {
            environment.getPropertySources().addAfter("systemEnvironment", source);
        } else {
            environment.getPropertySources().addFirst(source);
        }
    }
}
