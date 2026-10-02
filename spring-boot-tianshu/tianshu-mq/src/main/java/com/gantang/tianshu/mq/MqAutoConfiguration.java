package com.gantang.tianshu.mq;

import com.gantang.tianshu.mq.config.MqProperties;
import com.gantang.tianshu.mq.core.MqProducer;
import com.gantang.tianshu.mq.trace.MicrometerTracePropagator;
import com.gantang.tianshu.mq.trace.NoopTracePropagator;
import com.gantang.tianshu.mq.trace.TracePropagator;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * MQ 组件自动装配。
 *
 * <p>仅在类路径存在 {@link StreamBridge}（即引入了 spring-cloud-stream）且
 * {@code tianshu.mq.enabled} 未关闭时生效。具体 binder（RocketMQ / Kafka 等）
 * 由业务方按需引入，本配置不绑定任何具体中间件。</p>
 *
 * @author gantang
 */
@AutoConfiguration
@ConditionalOnClass(StreamBridge.class)
@ConditionalOnProperty(prefix = MqProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqProperties.class)
public class MqAutoConfiguration {

    /**
     * micrometer-tracing 在类路径时的链路装配：容器中存在 {@link Tracer} Bean
     * 使用 micrometer 实现，否则降级为 no-op。
     *
     * <p>独立为嵌套配置并加 {@link ConditionalOnClass}，是因为 micrometer-tracing
     * 为 optional 依赖；类路径缺失时整个配置跳过，避免方法签名引用缺失类导致
     * 自动配置加载失败。</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Tracer.class)
    static class MicrometerTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TracePropagator tracePropagator(ObjectProvider<Tracer> tracer) {
            Tracer instance = tracer.getIfAvailable();
            return instance == null ? new NoopTracePropagator()
                    : new MicrometerTracePropagator(instance);
        }
    }

    /**
     * micrometer-tracing 不在类路径时的兜底装配，按声明顺序在前一个嵌套配置
     * 未注册 {@link TracePropagator} 时生效。
     */
    @Configuration(proxyBeanMethods = false)
    static class NoopTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TracePropagator noopTracePropagator() {
            return new NoopTracePropagator();
        }
    }

    /**
     * 生产端封装。
     *
     * @param streamBridge    Spring Cloud Stream 动态发送器
     * @param properties      MQ 配置
     * @param tracePropagator 链路透传器
     * @param environment     环境配置，用于读取 spring.application.name 作为默认来源
     * @return 生产端
     */
    @Bean
    @ConditionalOnMissingBean
    public MqProducer mqProducer(StreamBridge streamBridge, MqProperties properties,
                                 TracePropagator tracePropagator, Environment environment) {
        String source = environment.getProperty("spring.application.name", "tianshu-unknown");
        return new MqProducer(streamBridge, properties, tracePropagator, source);
    }
}
