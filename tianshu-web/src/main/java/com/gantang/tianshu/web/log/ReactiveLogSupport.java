package com.gantang.tianshu.web.log;

import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Reactor 返回类型检测与日志挂载工具。Reactor 为可选依赖，按类名检测，
 * 因此在缺少 Reactor 时该类仍可加载。
 *
 * @author gantang
 */
final class ReactiveLogSupport {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ReactiveLogSupport.class);

    private ReactiveLogSupport() {
    }

    static boolean isReactive(Object result) {
        if (result == null) {
            return false;
        }
        String type = result.getClass().getName();
        return type.startsWith("reactor.core.publisher.Mono")
                || type.startsWith("reactor.core.publisher.Flux");
    }

    static Object advise(Object publisher, String name, long start) {
        try {
            if (publisher instanceof reactor.core.publisher.Mono<?> mono) {
                return mono
                        .doOnSuccess(v -> log.info("[Logger] <- {} completed in {} ms", name, elapsedMs(start)))
                        .doOnError(ex -> log.error("[Logger] <- {} failed in {} ms: {}",
                                name, elapsedMs(start), ex.getMessage(), ex));
            }
            if (publisher instanceof reactor.core.publisher.Flux<?> flux) {
                return flux
                        .doOnComplete(() -> log.info("[Logger] <- {} completed in {} ms", name, elapsedMs(start)))
                        .doOnError(ex -> log.error("[Logger] <- {} failed in {} ms: {}",
                                name, elapsedMs(start), ex.getMessage(), ex));
            }
        } catch (NoClassDefFoundError ignored) {
            // 运行时缺少 Reactor，原样返回。
        }
        return publisher;
    }

    private static long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
