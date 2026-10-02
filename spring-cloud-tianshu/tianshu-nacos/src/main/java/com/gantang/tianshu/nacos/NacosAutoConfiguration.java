package com.gantang.tianshu.nacos;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Nacos 注册发现自动配置（路线 2）。
 *
 * <p>仅基于官方 nacos-client，桥接 Spring Cloud Commons 的 {@code DiscoveryClient}：
 * <ul>
 *     <li>{@link NacosServiceManager}：持有 NamingService，负责注册/注销/查询；</li>
 *     <li>{@link NacosDiscoveryClient}：供 Gateway {@code lb://} 与 OpenFeign 负载均衡使用；</li>
 *     <li>{@link NacosAutoServiceRegistration}：启动自动注册、关闭自动注销。</li>
 * </ul>
 *
 * <p>通过 {@code tianshu.nacos.enabled=false} 可整体关闭。
 *
 * @author gantang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "tianshu.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NacosDiscoveryProperties.class)
public class NacosAutoConfiguration {

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean
    public NacosServiceManager nacosServiceManager(NacosDiscoveryProperties properties) {
        return new NacosServiceManager(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NacosDiscoveryClient nacosDiscoveryClient(NacosServiceManager manager) {
        return new NacosDiscoveryClient(manager);
    }

    @Bean
    @ConditionalOnClass(name = "org.reactivestreams.Publisher")
    @ConditionalOnMissingBean
    public ReactiveNacosDiscoveryClient reactiveNacosDiscoveryClient(NacosDiscoveryClient client) {
        return new ReactiveNacosDiscoveryClient(client);
    }

    @Bean
    @ConditionalOnMissingBean
    public NacosAutoServiceRegistration nacosAutoServiceRegistration(NacosDiscoveryProperties properties,
                                                                     NacosServiceManager manager,
                                                                     Environment environment) {
        return new NacosAutoServiceRegistration(properties, manager, environment);
    }
}
