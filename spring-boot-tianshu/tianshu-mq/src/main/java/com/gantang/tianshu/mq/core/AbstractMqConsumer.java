package com.gantang.tianshu.mq.core;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import tools.jackson.databind.ObjectMapper;
import com.gantang.tianshu.mq.trace.TracePropagator;

/**
 * MQ 消费端模板。
 *
 * <p>子类实现 {@link #getPayloadType()} 与 {@link #handle(MqMessage)} 即可，模板统一负责：</p>
 * <ul>
 *   <li>binder 反序列化后 payload 的二次类型转换（JSON 反序列化存在泛型擦除）；</li>
 *   <li>traceId 提取与消费 span 建立；</li>
 *   <li>幂等钩子 {@link #isDuplicate(MqMessage)}（默认不幂等，业务方按需实现，
 *       呼应"跨服务写消费端必须幂等"的约定）；</li>
 *   <li>异常日志收口，异常继续上抛由 binder 的 retry / DLQ 机制处理，模板不另造重试轮子。</li>
 * </ul>
 *
 * <p>子类声明为 Spring Bean 并命名为函数式 consumer，例如：</p>
 * <pre>{@code
 * @Bean
 * AbstractMqConsumer<OrderCreatedEvent> orderCreatedConsumer(...) {
 *     return new AbstractMqConsumer<>(...) { ... };
 * }
 * }</pre>
 * <p>对应 binding 名 {@code orderCreatedConsumer-in-0}。</p>
 *
 * @param <T> 业务载荷类型
 * @author gantang
 */
public abstract class AbstractMqConsumer<T> implements Consumer<Message<?>> {

    private static final Logger log = LoggerFactory.getLogger(AbstractMqConsumer.class);

    private final ObjectMapper objectMapper;

    private final TracePropagator tracePropagator;

    protected AbstractMqConsumer(ObjectMapper objectMapper, TracePropagator tracePropagator) {
        this.objectMapper = objectMapper;
        this.tracePropagator = tracePropagator;
    }

    /**
     * 业务载荷的实际类型，用于 JSON 反序列化后的类型转换。
     *
     * @return 载荷类型
     */
    protected abstract Class<T> getPayloadType();

    /**
     * 处理消息。抛出的异常会上抛以触发 binder 的重试 / 死信流程。
     *
     * @param message 消息信封
     */
    protected abstract void handle(MqMessage<T> message);

    /**
     * 幂等判断钩子，默认不做幂等控制。
     *
     * <p>业务方可基于 msgId / bizKey 查询消费记录，返回 true 时消息直接跳过。</p>
     *
     * @param message 消息信封
     * @return true 表示消息已处理过，应跳过
     */
    protected boolean isDuplicate(MqMessage<T> message) {
        return false;
    }

    @Override
    public void accept(Message<?> rawMessage) {
        Object payload = rawMessage.getPayload();

        MqMessage<?> envelope;
        Object rawPayload;
        if (payload instanceof MqMessage<?> mqMessage) {
            envelope = mqMessage;
            rawPayload = mqMessage.getPayload();
        } else {
            // 生产端关闭信封时，消费侧收到裸 payload，补一个临时信封
            envelope = MqMessage.of(null, payload);
            rawPayload = payload;
        }

        T converted = objectMapper.convertValue(rawPayload, getPayloadType());
        MqMessage<T> message = copyWith(envelope, converted);

        String traceId = (String) rawMessage.getHeaders()
                .get(TracePropagator.TRACE_ID_HEADER);
        String spanName = message.getEventType() != null ? message.getEventType()
                : getPayloadType().getSimpleName();

        try {
            tracePropagator.consume(spanName, traceId, () -> doHandle(message));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("MQ 消息消费失败: msgId=" + message.getMsgId(), ex);
        }
    }

    private void doHandle(MqMessage<T> message) {
        if (isDuplicate(message)) {
            log.info("MQ 消息重复，跳过处理: msgId={}, bizKey={}, eventType={}",
                    message.getMsgId(), message.getBizKey(), message.getEventType());
            return;
        }
        log.info("MQ 消息开始消费: msgId={}, bizKey={}, eventType={}",
                message.getMsgId(), message.getBizKey(), message.getEventType());
        handle(message);
        log.info("MQ 消息消费完成: msgId={}", message.getMsgId());
    }

    private MqMessage<T> copyWith(MqMessage<?> source, T payload) {
        return new MqMessage<>(source.getMsgId(), source.getBizKey(), source.getEventType(),
                source.getTimestamp(), source.getSource(), source.getTraceId(), payload);
    }
}
