package com.gantang.tianshu.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 增强自动配置：分页、乐观锁、审计字段填充，开箱即用。
 *
 * @author gantang
 */
@AutoConfiguration
@EnableConfigurationProperties(MybatisProperties.class)
public class MybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CurrentUserResolver currentUserResolver() {
        return new UserContextCurrentUserResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (properties.isPaginationEnabled()) {
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(properties.getDbType()));
        }
        if (properties.isOptimisticLockEnabled()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultMetaObjectHandler metaObjectHandler(CurrentUserResolver currentUserResolver) {
        return new DefaultMetaObjectHandler(currentUserResolver);
    }
}
