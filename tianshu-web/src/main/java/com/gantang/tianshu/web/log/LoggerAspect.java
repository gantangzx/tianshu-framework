package com.gantang.tianshu.web.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 方法日志切面：记录方法入参、执行耗时与异常，不依赖 Servlet API，故双栈通用。
 *
 * @author gantang
 */
@Aspect
public class LoggerAspect {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoggerAspect.class);

    @Around("@annotation(loggerAnnotation)")
    public Object around(ProceedingJoinPoint pjp, Logger loggerAnnotation) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String name = resolveName(loggerAnnotation, method);
        if (loggerAnnotation.args()) {
            log.info("[Logger] -> {} args={}", name, Arrays.toString(pjp.getArgs()));
        } else {
            log.info("[Logger] -> {}", name);
        }

        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            if (ReactiveLogSupport.isReactive(result)) {
                return ReactiveLogSupport.advise(result, name, start);
            }
            log.info("[Logger] <- {} completed in {} ms", name, elapsedMs(start));
            return result;
        } catch (Throwable ex) {
            log.error("[Logger] <- {} failed in {} ms: {}", name, elapsedMs(start), ex.getMessage(), ex);
            throw ex;
        }
    }

    private String resolveName(Logger loggerAnnotation, Method method) {
        if (!loggerAnnotation.value().isBlank()) {
            return loggerAnnotation.value();
        }
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    private static long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
