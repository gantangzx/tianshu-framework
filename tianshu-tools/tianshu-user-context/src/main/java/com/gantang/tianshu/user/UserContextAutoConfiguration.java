package com.gantang.tianshu.user;

import com.gantang.tianshu.user.core.JwtUserContextResolver;
import com.gantang.tianshu.user.core.UserContextResolver;
import com.gantang.tianshu.user.reactive.ReactiveUserContextFilter;
import com.gantang.tianshu.user.servlet.ServletUserContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-configuration wiring the stack-independent {@link UserContextResolver} and the matching
 * user-context filter for either a servlet or reactive web application.
 */
@AutoConfiguration
public class UserContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserContextResolver.class)
    public UserContextResolver jwtUserContextResolver() {
        return new JwtUserContextResolver();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletUserContextConfiguration {

        @Bean
        public FilterRegistrationBean<ServletUserContextFilter> servletUserContextFilter(
                UserContextResolver resolver) {
            FilterRegistrationBean<ServletUserContextFilter> registration =
                    new FilterRegistrationBean<>(new ServletUserContextFilter(resolver));
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
            return registration;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class ReactiveUserContextConfiguration {

        @Bean
        public ReactiveUserContextFilter reactiveUserContextFilter(UserContextResolver resolver) {
            return new ReactiveUserContextFilter(resolver);
        }
    }
}
