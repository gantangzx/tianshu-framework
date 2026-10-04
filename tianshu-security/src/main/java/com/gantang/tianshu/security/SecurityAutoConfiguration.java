package com.gantang.tianshu.security;

import com.gantang.tianshu.security.reactive.ReactiveSecurityConfig;
import com.gantang.tianshu.security.servlet.ServletSecurityConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    /**
     * Servlet 栈。通过 {@link Import} 导入 {@link ServletSecurityConfig}，
     * 使其被当作完整配置类处理，内部 {@code SecurityFilterChain} 才会注册。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    @Import(ServletSecurityConfig.class)
    static class ServletStack {
    }

    /**
     * Reactive 栈。同样通过 {@link Import} 导入 {@link ReactiveSecurityConfig}。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.DispatcherHandler")
    @Import(ReactiveSecurityConfig.class)
    static class ReactiveStack {
    }
}
