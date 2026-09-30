package com.gantang.tianshu.diagnose.log.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.gantang.tianshu.diagnose.api.DiagnoseConstants;
import com.gantang.tianshu.diagnose.api.ErrorEvent;
import com.gantang.tianshu.diagnose.log.ErrorFingerprinter;
import com.gantang.tianshu.diagnose.log.ErrorSecretMasker;
import com.gantang.tianshu.diagnose.log.sink.BufferingErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.ErrorEventSink;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 只拦 ERROR（可配含带异常 WARN）：组装 {@link ErrorEvent} → 指纹 → 脱敏 → 入有界队列，
 * 单 worker 线程异步发送。
 *
 * <p>红线：{@link #append} 仅内存操作，绝不阻塞、绝不向业务线程外抛；下游全坏只缓冲 + dropped。
 */
public class ErrorLogAppender extends AppenderBase<ILoggingEvent> {

    private static final String SELF_PACKAGE = "com.gantang.tianshu.diagnose";
    private static final String MDC_TRACE_ID = "traceId";

    private LinkedBlockingQueue<ErrorEvent> queue;
    private final AtomicLong dropped = new AtomicLong();
    private final ThreadLocal<Boolean> inWorker = ThreadLocal.withInitial(() -> false);

    private boolean includeWarnWithThrowable = false;
    private int stackTopN = 20;
    private int queueCapacity = 8192;
    private String businessPackages = "com.gantang";
    private String bufferDir = "./.tianshu-diagnose-buffer";
    private long maxBufferFileBytes = 64L * 1024 * 1024;
    private long maxBufferTotalBytes = 512L * 1024 * 1024;

    private String appName = "";
    private String env = "";
    private String host = "";

    private Thread worker;
    private volatile ErrorEventSink sink;
    private ErrorFingerprinter fingerprinter;
    private final ErrorSecretMasker masker = new ErrorSecretMasker();

    @Override
    public void start() {
        this.queue = new LinkedBlockingQueue<>(Math.max(1, queueCapacity));
        this.fingerprinter = new ErrorFingerprinter(splitCsv(businessPackages));
        this.host = resolveHost();
        if (sink == null) {
            // Spring 就绪前 sink 未注入：所有事件先落缓冲，恢复后重放。
            sink = new BufferingErrorEventSink(e -> ErrorEventSink.Result.RETRY,
                    java.nio.file.Paths.get(bufferDir), maxBufferFileBytes, maxBufferTotalBytes);
        }
        this.worker = new Thread(this::runWorker, "tianshu-diagnose-sender");
        this.worker.setDaemon(true);
        this.worker.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            if (!shouldCapture(event)) {
                return;
            }
            String loggerName = event.getLoggerName();
            if (loggerName != null && loggerName.startsWith(SELF_PACKAGE)) {
                return;
            }
            if (Boolean.TRUE.equals(inWorker.get())) {
                return;
            }
            ErrorEvent errorEvent = convert(event);
            if (!queue.offer(errorEvent)) {
                dropped.incrementAndGet();
            }
        } catch (RuntimeException e) {
            // 绝不让采集影响业务线程
            dropped.incrementAndGet();
        }
    }

    private boolean shouldCapture(ILoggingEvent event) {
        int level = event.getLevel().toInt();
        if (level >= ch.qos.logback.classic.Level.ERROR_INT) {
            return true;
        }
        return includeWarnWithThrowable
                && level >= ch.qos.logback.classic.Level.WARN_INT
                && event.getThrowableProxy() != null;
    }

    private ErrorEvent convert(ILoggingEvent event) {
        List<String> frames = new ArrayList<>();
        String exceptionClass = null;

        var throwable = event.getThrowableProxy();
        if (throwable != null) {
            exceptionClass = throwable.getClassName();
            StackTraceElementProxy[] proxyFrames = throwable.getStackTraceElementProxyArray();
            if (proxyFrames != null) {
                int limit = Math.min(stackTopN, proxyFrames.length);
                for (int i = 0; i < limit; i++) {
                    frames.add(String.valueOf(proxyFrames[i]));
                }
            }
        }

        String message = masker.mask(event.getFormattedMessage());
        List<String> maskedFrames = new ArrayList<>(frames.size());
        for (String frame : frames) {
            maskedFrames.add(masker.mask(frame));
        }

        String traceId = traceIdOf(event);
        String fingerprint = fingerprinter.fingerprint(
                exceptionClass, maskedFrames, null, event.getLoggerName(), event.getFormattedMessage());

        ErrorEvent e = new ErrorEvent();
        e.setSchemaVersion(DiagnoseConstants.SCHEMA_VERSION);
        e.setMsgId(UUID.randomUUID().toString().replace("-", ""));
        e.setTraceId(traceId);
        e.setAppName(appName);
        e.setEnv(env);
        e.setHost(host);
        e.setLevel(event.getLevel().toString());
        e.setLogger(event.getLoggerName());
        e.setExceptionClass(exceptionClass);
        e.setMessage(message);
        e.setStackFrames(maskedFrames);
        e.setFingerprint(fingerprint);
        e.setTimestamp(event.getTimeStamp());
        return e;
    }

    private void runWorker() {
        inWorker.set(true);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ErrorEvent event = queue.poll(500, TimeUnit.MILLISECONDS);
                if (event == null) {
                    continue;
                }
                ErrorEventSink current = sink;
                if (current != null) {
                    try {
                        current.publish(event);
                    } catch (RuntimeException e) {
                        dropped.incrementAndGet();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            inWorker.remove();
        }
    }

    @Override
    public void stop() {
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        super.stop();
    }

    private String traceIdOf(ILoggingEvent event) {
        try {
            var map = event.getMDCPropertyMap();
            return map == null ? null : map.get(MDC_TRACE_ID);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /** 由 Spring 侧 AppenderBinder 注入选中的传输（已包缓冲装饰）。 */
    public void setSink(ErrorEventSink sink) {
        this.sink = sink;
    }

    public long droppedCount() {
        return dropped.get();
    }

    // ---- logback xml / binder 可调项 ----
    public void setIncludeWarnWithThrowable(boolean v) { this.includeWarnWithThrowable = v; }
    public void setStackTopN(int v) { this.stackTopN = v; }
    public void setQueueCapacity(int v) { this.queueCapacity = v; }
    public void setBusinessPackages(String v) { this.businessPackages = v; }
    public void setBufferDir(String v) { this.bufferDir = v; }
    public void setMaxBufferFileBytes(long v) { this.maxBufferFileBytes = v; }
    public void setMaxBufferTotalBytes(long v) { this.maxBufferTotalBytes = v; }
    public void setAppName(String v) { this.appName = v == null ? "" : v; }
    public void setEnv(String v) { this.env = v == null ? "" : v; }
}
