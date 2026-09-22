package com.gantang.tianshu.security.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gantang.tianshu.common.api.R;
import com.gantang.tianshu.common.api.ResultCode;
import com.gantang.tianshu.security.SecurityProperties;
import com.gantang.tianshu.security.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

/**
 * Reactive 安全过滤器链配置。
 *
 * @author gantang
 */
@EnableWebFluxSecurity
public class ReactiveSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, SecurityProperties properties,
                                                         TokenService tokenService, ObjectMapper objectMapper) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(properties.getPermitAll().toArray(String[]::new)).permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, denied) -> writeJson(exchange,
                                HttpStatus.UNAUTHORIZED, ResultCode.UNAUTHORIZED, objectMapper))
                        .accessDeniedHandler((exchange, denied) -> writeJson(exchange,
                                HttpStatus.FORBIDDEN, ResultCode.FORBIDDEN, objectMapper)))
                .addFilterAt(new ReactiveJwtAuthenticationFilter(tokenService),
                        SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    private static Mono<Void> writeJson(org.springframework.web.server.ServerWebExchange exchange,
                                        HttpStatus status, com.gantang.tianshu.common.api.IResultCode resultCode,
                                        ObjectMapper objectMapper) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(R.fail(resultCode));
        } catch (Exception ex) {
            return Mono.error(ex);
        }
        var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
