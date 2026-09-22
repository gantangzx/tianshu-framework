package com.gantang.tianshu.security.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gantang.tianshu.common.api.R;
import com.gantang.tianshu.common.api.ResultCode;
import com.gantang.tianshu.common.web.ServletWebSupport;
import com.gantang.tianshu.security.SecurityProperties;
import com.gantang.tianshu.security.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Servlet 安全过滤器链配置。
 *
 * @author gantang
 */
@EnableWebSecurity
public class ServletSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties,
                                                   TokenService tokenService, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(properties.getPermitAll().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                ServletWebSupport.writeJson(response, ResultCode.UNAUTHORIZED.getCode(),
                                        objectMapper.writeValueAsString(R.fail(ResultCode.UNAUTHORIZED))))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                ServletWebSupport.writeJson(response, ResultCode.FORBIDDEN.getCode(),
                                        objectMapper.writeValueAsString(R.fail(ResultCode.FORBIDDEN)))))
                .addFilterBefore(new JwtAuthenticationFilter(tokenService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
