package com.gantang.tianshu.mq.trace;

/**
 * 无 tracing 实现时的降级实现：不建立 span，业务逻辑直接执行。
 *
 * @author gantang
 */
public class NoopTracePropagator implements TracePropagator {

    @Override
    public String currentTraceId() {
        return null;
    }

    @Override
    public void consume(String spanName, String traceId, CheckedRunnable operation) throws Exception {
        operation.run();
    }
}
