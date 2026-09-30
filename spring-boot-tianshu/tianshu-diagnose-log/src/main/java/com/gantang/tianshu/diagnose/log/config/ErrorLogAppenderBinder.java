package com.gantang.tianshu.diagnose.log.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.gantang.tianshu.diagnose.log.appender.ErrorLogAppender;
import com.gantang.tianshu.diagnose.log.sink.ErrorEventSink;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * 延迟绑定：logback 完成初始化后，把组装好的 sink 绑定到 root 上的诊断 appender。幂等。
 */
public class ErrorLogAppenderBinder {

    private static final String APPENDER_NAME = "TIANSHU_DIAGNOSE";

    private final DiagnoseLogProperties props;
    private final Environment environment;

    public ErrorLogAppenderBinder(DiagnoseLogProperties props, Environment environment) {
        this.props = props;
        this.environment = environment;
    }

    /**
     * @param sink 已包缓冲装饰的主传输
     * @return true 表示绑定成功；非 logback 环境返回 false
     */
    public boolean bind(ErrorEventSink sink) {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (!(factory instanceof LoggerContext context)) {
            return false;
        }
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        ErrorLogAppender appender = (ErrorLogAppender) root.getAppender(APPENDER_NAME);
        if (appender == null) {
            appender = new ErrorLogAppender();
            appender.setName(APPENDER_NAME);
            appender.setContext(context);
            root.addAppender(appender);
        }
        applyProperties(appender, sink);
        if (!appender.isStarted()) {
            appender.start();
        }
        return true;
    }

    private void applyProperties(ErrorLogAppender appender, ErrorEventSink sink) {
        appender.setSink(sink);
        appender.setIncludeWarnWithThrowable(props.isIncludeWarnWithThrowable());
        appender.setStackTopN(props.getStackTopN());
        appender.setQueueCapacity(props.getQueueCapacity());
        appender.setBusinessPackages(props.getBusinessPackages());
        appender.setBufferDir(props.getBufferDir());
        appender.setMaxBufferFileBytes(props.getMaxBufferFileBytes());
        appender.setMaxBufferTotalBytes(props.getMaxBufferTotalBytes());
        appender.setAppName(appName());
        appender.setEnv(activeProfiles());
    }

    private String appName() {
        String name = environment.getProperty("spring.application.name");
        return name == null ? "" : name;
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        return profiles == null ? "" : String.join(",", Arrays.asList(profiles));
    }
}
