package com.gantang.tianshu.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQ 组件配置项。
 *
 * <p>具体 binder（RocketMQ / Kafka 等）的连接与 binding 配置仍遵循 Spring Cloud Stream
 * 原生约定（{@code spring.cloud.stream.*}），本类只承载框架增强能力的开关。</p>
 *
 * @author gantang
 */
@Data
@ConfigurationProperties(MqProperties.PREFIX)
public class MqProperties {

    public static final String PREFIX = "tianshu.mq";

    /**
     * 是否启用 MQ 组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 是否在生产端自动包装统一消息信封 {@code MqMessage}。
     *
     * <p>关闭后 {@code MqProducer} 直接发送业务 payload，用于对接历史 topic。</p>
     */
    private boolean envelopeEnabled = true;

    /**
     * 是否启用 traceId 透传（生产写入消息头，消费恢复链路）。
     */
    private boolean traceEnabled = true;

    /**
     * 消息来源标识，写入信封 source 字段，缺省取 spring.application.name。
     */
    private String source;
}
