package com.gantang.tianshu.diagnose.log.sink;

import com.gantang.tianshu.diagnose.api.ErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓冲装饰：仅在 worker 线程使用。下游成功时先重放缓冲再发当前事件；下游失败/未就绪时
 * 把事件以 JSONL 追加本地缓冲。受单文件 / 总字节上限保护，超限只计 {@code dropped}，
 * 绝不向业务线程外抛。
 */
public class BufferingErrorEventSink implements ErrorEventSink {

    private static final Logger log = LoggerFactory.getLogger("com.gantang.tianshu.diagnose.ALERT");

    private final ErrorEventSink delegate;
    private final Path bufferDir;
    private final long maxFileBytes;
    private final long maxTotalBytes;
    private final AtomicLong dropped = new AtomicLong();

    public BufferingErrorEventSink(ErrorEventSink delegate, Path bufferDir,
                                   long maxFileBytes, long maxTotalBytes) {
        this.delegate = delegate;
        this.bufferDir = bufferDir;
        this.maxFileBytes = maxFileBytes;
        this.maxTotalBytes = maxTotalBytes;
    }

    public long droppedCount() {
        return dropped.get();
    }

    /** 被装饰的主传输（主要用于装配校验/测试）。 */
    public ErrorEventSink delegate() {
        return delegate;
    }

    @Override
    public Result publish(ErrorEvent event) {
        try {
            replay();
        } catch (RuntimeException e) {
            log.warn("buffer replay failed: {}", e.toString());
        }
        Result result = safeDelegate(event);
        if (result == null || result.isRetryable()) {
            buffer(event);
            return Result.RETRY;
        }
        if (!result.isSuccess()) {
            dropped.incrementAndGet();
        }
        return result;
    }

    private Result safeDelegate(ErrorEvent event) {
        try {
            return delegate.publish(event);
        } catch (RuntimeException e) {
            log.warn("sink publish failed, will buffer: {}", e.toString());
            return Result.RETRY;
        }
    }

    private void replay() {
        Path file = bufferFile();
        if (!Files.exists(file) || size(file) == 0) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        boolean allOk = true;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            try {
                ErrorEvent buffered = ErrorEventJson.MAPPER.readValue(line, ErrorEvent.class);
                Result r = safeDelegate(buffered);
                if (r == null || r.isRetryable()) {
                    allOk = false;
                    break;
                }
            } catch (RuntimeException e) {
                allOk = false;
                break;
            }
        }
        if (allOk) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("failed to delete buffer file: {}", e.toString());
            }
        }
    }

    private void buffer(ErrorEvent event) {
        try {
            ensureDir();
            Path file = bufferFile();
            String line = ErrorEventJson.toJson(event) + System.lineSeparator();
            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            long currentSize = Files.exists(file) ? size(file) : 0;
            long totalSize = totalBufferBytes();
            if (currentSize + bytes.length > maxFileBytes || totalSize + bytes.length > maxTotalBytes) {
                dropped.incrementAndGet();
                log.warn("diagnose buffer limit reached (file={}, total={}), dropped event msgId={}",
                        currentSize, totalSize, event.getMsgId());
                return;
            }
            Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            dropped.incrementAndGet();
            log.warn("failed to buffer error event: {}", e.toString());
        }
    }

    private void ensureDir() throws IOException {
        if (!Files.exists(bufferDir)) {
            Files.createDirectories(bufferDir);
        }
    }

    private Path bufferFile() {
        return bufferDir.resolve("error-events.jsonl");
    }

    private long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private long totalBufferBytes() {
        try (var stream = Files.list(bufferDir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(this::size)
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }
}
