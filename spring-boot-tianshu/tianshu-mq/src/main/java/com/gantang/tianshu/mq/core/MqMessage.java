package com.gantang.tianshu.mq.core;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一消息信封。
 *
 * <p>所有经 {@code MqProducer} 发送的消息默认包装为本结构，承载消息元数据（消息 ID、
 * 业务键、事件类型、时间戳、来源、traceId）与业务 payload，消费端可据此做幂等与
 * 链路关联，禁止业务方另造平行的消息体。</p>
 *
 * @param <T> 业务载荷类型
 * @author gantang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一 ID，由生产端生成，用于消费端幂等。
     */
    private String msgId;

    /**
     * 业务键（如订单号），便于按业务维度检索与排障，可为空。
     */
    private String bizKey;

    /**
     * 事件类型，表达业务语义（如 order-created）。
     */
    private String eventType;

    /**
     * 生产时间戳（毫秒）。
     */
    private Long timestamp;

    /**
     * 消息来源（通常为发送方应用名）。
     */
    private String source;

    /**
     * 链路 traceId，用于跨服务串联。
     */
    private String traceId;

    /**
     * 业务载荷。
     */
    private T payload;

    /**
     * 构造仅指定事件类型与载荷的消息，msgId 自动生成。
     *
     * @param eventType 事件类型
     * @param payload   业务载荷
     * @param <T>       载荷类型
     * @return 消息信封
     */
    public static <T> MqMessage<T> of(String eventType, T payload) {
        return of(eventType, null, payload);
    }

    /**
     * 构造指定事件类型、业务键与载荷的消息，msgId 自动生成。
     *
     * @param eventType 事件类型
     * @param bizKey    业务键
     * @param payload   业务载荷
     * @param <T>       载荷类型
     * @return 消息信封
     */
    public static <T> MqMessage<T> of(String eventType, String bizKey, T payload) {
        MqMessage<T> message = new MqMessage<>();
        message.msgId = UUID.randomUUID().toString().replace("-", "");
        message.bizKey = bizKey;
        message.eventType = eventType;
        message.timestamp = System.currentTimeMillis();
        message.payload = payload;
        return message;
    }
}
