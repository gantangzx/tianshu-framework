package com.gantang.tianshu.diagnose.log.sink;

import com.gantang.tianshu.diagnose.api.ErrorEvent;

/**
 * 错误事件传输 SPI。所有实现（MQ / HTTP / Logging）以及缓冲装饰都实现该接口。
 *
 * <p>唯一方法 {@link #publish} 由单 worker 线程调用；实现内部必须吞掉所有异常并自行记录，
 * 通过返回的 {@link Result} 表达成功 / 可重试失败（触发上层缓冲）。
 */
@FunctionalInterface
public interface ErrorEventSink {

    /**
     * 发布一条事件。
     *
     * @param event 已组装并脱敏的事件
     * @return 发布结果，{@code null} 视同 {@link Result#RETRY}
     */
    Result publish(ErrorEvent event);

    /** 发布结果。 */
    final class Result {

        public static final Result OK = new Result(true, false);
        public static final Result RETRY = new Result(false, true);
        /** 永久失败：不缓冲、仅计 dropped（如序列化不出）。 */
        public static final Result DROP = new Result(false, false);

        private final boolean success;
        private final boolean retryable;

        private Result(boolean success, boolean retryable) {
            this.success = success;
            this.retryable = retryable;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
