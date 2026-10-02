package com.gantang.tianshu.mq.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import com.gantang.tianshu.mq.config.MqProperties;
import com.gantang.tianshu.mq.trace.TracePropagator;

/**
 * MQ 生产端封装。
 *
 * <p>基于 {@link StreamBridge} 向动态 destination 发送消息，默认包装统一信封
 * {@link MqMessage}，并在开启链路追踪时自动写入 traceId（信封字段 + 消息头）。
 * 业务方只需注入本类，无需直接操作 StreamBridge 与消息构造细节。</p>
 *
 * @author gantang
 */
public class MqProducer {

    private static final Logger log = LoggerFactory.getLogger(MqProducer.class);

    private final StreamBridge streamBridge;

    private final MqProperties properties;

    private final TracePropagator tracePropagator;

    /**
     * 默认消息来源（通常为 spring.application.name）。
     */
    private final String defaultSource;

    public MqProducer(StreamBridge streamBridge, MqProperties properties,
                      TracePropagator tracePropagator, String defaultSource) {
        this.streamBridge = streamBridge;
        this.properties = properties;
        this.tracePropagator = tracePropagator;
        this.defaultSource = defaultSource;
    }

    /**
     * 发送消息，事件类型默认取 destination。
     *
     * @param destination 目标 destination（binder 会映射到实际 topic）
     * @param payload     业务载荷
     * @return 是否发送成功
     */
    public boolean send(String destination, Object payload) {
        return send(destination, destination, null, payload);
    }

    /**
     * 发送指定事件类型的消息。
     *
     * @param destination 目标 destination
     * @param eventType   事件类型
     * @param payload     业务载荷
     * @return 是否发送成功
     */
    public boolean send(String destination, String eventType, Object payload) {
        return send(destination, eventType, null, payload);
    }

    /**
     * 发送指定事件类型与业务键的消息。
     *
     * @param destination 目标 destination
     * @param eventType   事件类型
     * @param bizKey      业务键（如订单号），可为空
     * @param payload     业务载荷
     * @return 是否发送成功
     */
    public boolean send(String destination, String eventType, String bizKey, Object payload) {
        Assert.hasText(destination, "destination 不能为空");
        Assert.notNull(payload, "payload 不能为 null");
        return doSend(destination, MqMessage.of(eventType, bizKey, payload), payload);
    }

    /**
     * 直接发送已构造的消息信封。
     *
     * @param destination 目标 destination
     * @param message     消息信封
     * @return 是否发送成功
     */
    public boolean send(String destination, MqMessage<?> message) {
        Assert.hasText(destination, "destination 不能为空");
        Assert.notNull(message, "message 不能为 null");
        return doSend(destination, message, message.getPayload());
    }

    private boolean doSend(String destination, MqMessage<?> envelope, Object rawPayload) {
        String traceId = properties.isTraceEnabled() ? tracePropagator.currentTraceId() : null;

        Object body;
        if (properties.isEnvelopeEnabled()) {
            envelope.setSource(resolveSource());
            if (traceId != null) {
                envelope.setTraceId(traceId);
            }
            body = envelope;
        } else {
            body = rawPayload;
        }

        MessageBuilder<Object> builder = MessageBuilder.withPayload(body);
        if (traceId != null) {
            builder.setHeader(TracePropagator.TRACE_ID_HEADER, traceId);
        }
        Message<Object> message = builder.build();

        boolean result = streamBridge.send(destination, message);
        if (log.isDebugEnabled()) {
            log.debug("MQ 消息发送完成: destination={}, eventType={}, msgId={}, result={}",
                    destination, envelope.getEventType(), envelope.getMsgId(), result);
        }
        return result;
    }

    private String resolveSource() {
        String source = properties.getSource();
        return (source == null || source.isBlank()) ? defaultSource : source;
    }
}
