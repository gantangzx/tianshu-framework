package com.gantang.tianshu.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 下游服务 OpenAPI 自动配置。自动暴露 {@code /v3/api-docs} 与 {@code /swagger-ui.html}，
 * 文档标题默认取应用名，可通过 {@code tianshu.openapi.title / version / description} 覆盖。
 *
 * <p>配合 {@code tianshu-gateway} 即可在网关聚合查看全部服务接口。</p>
 *
 * @author gantang
 */
@AutoConfiguration
public class OpenApiAutoConfiguration {

    @Bean
    public OpenAPI tianshuOpenApi(
            @Value("${tianshu.openapi.title:${spring.application.name:Tianshu Service}}") String title,
            @Value("${tianshu.openapi.version:${spring.application.version:1.0.0}}") String version,
            @Value("${tianshu.openapi.description:}") String description) {
        return new OpenAPI().info(new Info().title(title).version(version).description(description));
    }
}
