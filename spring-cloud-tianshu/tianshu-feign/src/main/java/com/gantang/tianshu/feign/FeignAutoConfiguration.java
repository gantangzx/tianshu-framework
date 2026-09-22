package com.gantang.tianshu.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * OpenFeign 增强自动配置：注册请求头透传拦截器，引入即生效。
 *
 * @author gantang
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor tianshuFeignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }
}
