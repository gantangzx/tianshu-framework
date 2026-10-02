package com.gantang.tianshu.mq.trace;

/**
 * MQ 链路透传抽象。
 *
 * <p>将框架代码与具体 tracing 实现解耦：类路径存在 micrometer-tracing 时由
 * {@code MicrometerTracePropagator} 实现，否则使用 no-op 实现，仅做消息头透传。</p>
 *
 * @author gantang
 */
public interface TracePropagator {

    /**
     * 消息头中承载 traceId 的字段名。
     */
    String TRACE_ID_HEADER = "x-trace-id";

    /**
     * 获取当前线程绑定的 traceId，不存在时返回 {@code null}。
     *
     * @return 当前 traceId
     */
    String currentTraceId();

    /**
     * 在消费侧执行业务处理，tracing 实现可用其建立消费 span。
     *
     * @param spanName  span 名称（通常为事件类型）
     * @param traceId   生产端透传的 traceId，可能为 {@code null}
     * @param operation 业务处理
     * @throws Exception 业务处理抛出的异常原样抛出
     */
    void consume(String spanName, String traceId, CheckedRunnable operation) throws Exception;
}
