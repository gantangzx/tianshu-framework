package com.gantang.tianshu.diagnose.log.sink;

import com.gantang.tianshu.diagnose.api.ErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 兜底传输：输出结构化 JSON 到独立 logger（{@code com.gantang.tianshu.diagnose.ALERT}），
 * 可被独立文件/采集器接管。永远不向业务线程抛异常。
 */
public class LoggingErrorEventSink implements ErrorEventSink {

    private final Logger alertLogger;

    public LoggingErrorEventSink() {
        this(LoggerFactory.getLogger("com.gantang.tianshu.diagnose.ALERT"));
    }

    LoggingErrorEventSink(Logger alertLogger) {
        this.alertLogger = alertLogger;
    }

    @Override
    public Result publish(ErrorEvent event) {
        try {
            alertLogger.warn(ErrorEventJson.toJson(event));
            return Result.OK;
        } catch (RuntimeException e) {
            return Result.DROP;
        }
    }
}
