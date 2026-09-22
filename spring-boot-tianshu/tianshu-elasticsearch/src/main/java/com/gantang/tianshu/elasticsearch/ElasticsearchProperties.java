package com.gantang.tianshu.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the framework Elasticsearch helpers. Connection
 * settings remain under Spring Boot's standard {@code spring.elasticsearch.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = ElasticsearchProperties.PREFIX)
public class ElasticsearchProperties {

    public static final String PREFIX = "tianshu.elasticsearch";

    /**
     * Whether the framework {@link ElasticsearchService} is registered.
     */
    private boolean enabled = true;
}
