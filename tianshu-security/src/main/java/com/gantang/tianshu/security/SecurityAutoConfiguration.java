package com.gantang.tianshu.security;

import com.gantang.tianshu.security.reactive.ReactiveSecurityConfig;
import com.gantang.tianshu.security.servlet.ServletSecurityConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 安全组件自动配置。默认开启，可通过 {@code tianshu.security.enabled=false} 关闭。
 *
 * @author gantang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = SecurityProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
@Import({SecurityAutoConfiguration.ServletStack.class, SecurityAutoConfiguration.ReactiveStack.class})
public class SecurityAutoConfiguration {

    @Bean
    public TokenService tokenService(SecurityProperties properties) {
        return new TokenService(properties);
    }

    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    static class ServletStack {

        @org.springframework.context.annotation.Bean
        public ServletSecurityConfig servletSecurityConfig() {
            return new ServletSecurityConfig();
        }
    }

    @ConditionalOnClass(name = "org.springframework.web.reactive.DispatcherHandler")
    static class ReactiveStack {

        @org.springframework.context.annotation.Bean
        public ReactiveSecurityConfig reactiveSecurityConfig() {
            return new ReactiveSecurityConfig();
        }
    }
}
