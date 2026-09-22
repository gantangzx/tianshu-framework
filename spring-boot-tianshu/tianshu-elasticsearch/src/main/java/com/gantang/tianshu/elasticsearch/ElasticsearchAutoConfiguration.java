package com.gantang.tianshu.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the framework Elasticsearch helpers. The connection and the
 * {@link ElasticsearchClient} bean are provided by Spring Boot's own Elasticsearch
 * auto-configuration; this class only adds the convenience {@link ElasticsearchService}.
 */
@AutoConfiguration
@ConditionalOnClass(ElasticsearchClient.class)
@EnableConfigurationProperties(ElasticsearchProperties.class)
@ConditionalOnProperty(prefix = ElasticsearchProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ElasticsearchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ElasticsearchClient.class)
    public ElasticsearchService elasticsearchService(ElasticsearchClient client) {
        return new ElasticsearchService(client);
    }
}
