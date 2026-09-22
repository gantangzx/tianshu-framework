package com.gantang.tianshu.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gantang.tianshu.gateway.doc.SwaggerAggregationController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 网关自动配置。除 Spring Cloud Gateway 默认能力外，按开关装配 Swagger 文档聚合。
 *
 * @author gantang
 */
@AutoConfiguration
@ConditionalOnClass(ReactiveDiscoveryClient.class)
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder gatewayWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @ConditionalOnProperty(prefix = GatewayProperties.PREFIX, name = "doc-enabled",
            havingValue = "true", matchIfMissing = true)
    public SwaggerAggregationController swaggerAggregationController(ReactiveDiscoveryClient discoveryClient,
                                                                     WebClient.Builder webClientBuilder,
                                                                     GatewayProperties properties,
                                                                     ObjectMapper objectMapper) {
        return new SwaggerAggregationController(discoveryClient, webClientBuilder, properties, objectMapper);
    }
}
