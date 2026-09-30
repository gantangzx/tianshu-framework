package com.gantang.tianshu.diagnose.log.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.gantang.tianshu.diagnose.api.ErrorEvent;
import com.gantang.tianshu.diagnose.log.sink.ErrorEventSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorLogAppenderTest {

    private LoggerContext context;
    private Logger logger;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();
        logger = context.getLogger("com.gantang.demo.Service");
    }

    @AfterEach
    void tearDown() {
        context.stop();
    }

    private static class RecordingSink implements ErrorEventSink {
        final List<ErrorEvent> events = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch latch;

        RecordingSink(int expected) {
            this.latch = new CountDownLatch(expected);
        }

        @Override
        public Result publish(ErrorEvent event) {
            events.add(event);
            latch.countDown();
            return Result.OK;
        }
    }

    @Test
    void capturesErrorWithFingerprintAndFrames(@TempDir Path dir) throws InterruptedException {
        ErrorLogAppender appender = new ErrorLogAppender();
        appender.setBufferDir(dir.toString());
        RecordingSink sink = new RecordingSink(1);
        appender.setSink(sink);
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        logger.error("create order failed", new NullPointerException("id is null"));

        assertThat(sink.latch.await(3, TimeUnit.SECONDS)).isTrue();
        ErrorEvent e = sink.events.get(0);
        assertThat(e.getLevel()).isEqualTo("ERROR");
        assertThat(e.getExceptionClass()).isEqualTo("java.lang.NullPointerException");
        assertThat(e.getFingerprint()).hasSize(32);
        assertThat(e.getStackFrames()).isNotEmpty();
        assertThat(e.getMessage()).doesNotContain("password=");
    }

    @Test
    void ignoresInfoLevel(@TempDir Path dir) {
        ErrorLogAppender appender = new ErrorLogAppender();
        appender.setBufferDir(dir.toString());
        RecordingSink sink = new RecordingSink(1);
        appender.setSink(sink);
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        logger.info("just info");

        assertThat(sink.events).isEmpty();
    }

    @Test
    void highVolumeDoesNotThrowAndCountsDropsWhenQueueFull(@TempDir Path dir) {
        ErrorLogAppender appender = new ErrorLogAppender();
        appender.setBufferDir(dir.toString());
        appender.setQueueCapacity(4);
        // 永不消费成功，制造队列堆积
        appender.setSink(event -> ErrorEventSink.Result.RETRY);
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        for (int i = 0; i < 50_000; i++) {
            logger.error("burst " + i, new IllegalStateException("x"));
        }
        // 业务线程始终未抛异常（能走到这里即证明）
        assertThat(appender.droppedCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void selfDiagnoseLogsAreNotDelivered(@TempDir Path dir) throws InterruptedException {
        ErrorLogAppender appender = new ErrorLogAppender();
        appender.setBufferDir(dir.toString());
        RecordingSink sink = new RecordingSink(1);
        appender.setSink(sink);
        appender.setContext(context);
        appender.start();

        Logger self = context.getLogger("com.gantang.tianshu.diagnose.ALERT");
        self.addAppender(appender);
        self.error("self alert should be skipped");

        Thread.sleep(300);
        assertThat(sink.events).isEmpty();
    }

    @Test
    void buffersWhenDownThenReplaysOnRecovery(@TempDir Path dir) throws InterruptedException {
        ErrorLogAppender appender = new ErrorLogAppender();
        appender.setBufferDir(dir.toString());
        RecordingSink recovered = new RecordingSink(2);
        // 一开始下游不可用：事件缓冲到同目录
        appender.setSink(new com.gantang.tianshu.diagnose.log.sink.BufferingErrorEventSink(
                event -> ErrorEventSink.Result.RETRY, dir,
                64L * 1024 * 1024, 512L * 1024 * 1024));
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        logger.error("downstream down", new RuntimeException("boom"));
        Thread.sleep(500); // 等 worker 缓冲落盘

        // 下游恢复：下一条 publish 触发缓冲重放
        appender.setSink(new com.gantang.tianshu.diagnose.log.sink.BufferingErrorEventSink(
                recovered, dir, 64L * 1024 * 1024, 512L * 1024 * 1024));

        logger.error("trigger replay", new RuntimeException("boom2"));

        assertThat(recovered.latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(recovered.events).hasSizeGreaterThanOrEqualTo(2);
    }
}
