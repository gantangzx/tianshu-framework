package com.gantang.tianshu.mq.trace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * 基于 micrometer-tracing 的链路实现。
 *
 * <p>生产侧读取当前 span 的 traceId；消费侧建立独立消费 span，并将上游透传的
 * traceId 记录为标签（仅透传 traceId 无法还原完整 span 父子关系，故不做伪续接）。</p>
 *
 * @author gantang
 */
public class MicrometerTracePropagator implements TracePropagator {

    private static final String UPSTREAM_TRACE_ID_TAG = "upstream.trace.id";

    private final Tracer tracer;

    public MicrometerTracePropagator(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public String currentTraceId() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan == null ? null : currentSpan.context().traceId();
    }

    @Override
    public void consume(String spanName, String traceId, CheckedRunnable operation) throws Exception {
        Span span = tracer.nextSpan().name(spanName);
        if (traceId != null && !traceId.isBlank()) {
            span.tag(UPSTREAM_TRACE_ID_TAG, traceId);
        }
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            operation.run();
        } catch (Exception ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}
