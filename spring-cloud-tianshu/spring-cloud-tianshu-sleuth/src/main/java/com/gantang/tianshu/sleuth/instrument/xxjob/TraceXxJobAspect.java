package com.gantang.tianshu.sleuth.instrument.xxjob;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Wraps XXL-Job handler executions in a dedicated trace span so that scheduled
 * background work is observable even when there is no inbound request context.
 */
@Aspect
public class TraceXxJobAspect {

    private static final String CLASS_KEY = "class";

    private static final String METHOD_KEY = "method";

    private final Tracer tracer;

    public TraceXxJobAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    @Around("execution (@com.xxl.job.core.handler.annotation.XxlJob  * *.*(..))")
    public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
        String spanName = toLowerHyphen(pjp.getSignature().getName());
        Span span = this.tracer.nextSpan().name(spanName);
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            span.tag(CLASS_KEY, pjp.getTarget().getClass().getSimpleName());
            span.tag(METHOD_KEY, pjp.getSignature().getName());
            return pjp.proceed();
        } catch (Throwable ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    private static String toLowerHyphen(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
