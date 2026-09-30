package com.gantang.tianshu.diagnose.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 采集开关与参数，前缀 {@code tianshu.diagnose}。
 */
@ConfigurationProperties(prefix = "tianshu.diagnose")
public class DiagnoseLogProperties {

    /** 总开关。 */
    private boolean enabled = true;

    /** 主传输：mq | http | logging。 */
    private String transport = "mq";

    /** 是否把带异常的 WARN 一并采集。 */
    private boolean includeWarnWithThrowable = false;

    /** 堆栈保留帧数。 */
    private int stackTopN = 20;

    /** appender 内部队列容量，满则丢弃并计数。 */
    private int queueCapacity = 8192;

    /** 业务包前缀（逗号分隔），用于指纹首帧选择。 */
    private String businessPackages = "com.gantang";

    /** 本地缓冲目录。 */
    private String bufferDir = "./.tianshu-diagnose-buffer";

    private long maxBufferFileBytes = 64L * 1024 * 1024;
    private long maxBufferTotalBytes = 512L * 1024 * 1024;

    private final Mq mq = new Mq();
    private final Http http = new Http();

    public static class Mq {
        private String topic = "error-log";

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
    }

    public static class Http {
        private String endpoint = "";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 3000;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int v) { this.connectTimeoutMs = v; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int v) { this.readTimeoutMs = v; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTransport() { return transport; }
    public void setTransport(String transport) { this.transport = transport; }
    public boolean isIncludeWarnWithThrowable() { return includeWarnWithThrowable; }
    public void setIncludeWarnWithThrowable(boolean v) { this.includeWarnWithThrowable = v; }
    public int getStackTopN() { return stackTopN; }
    public void setStackTopN(int v) { this.stackTopN = v; }
    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int v) { this.queueCapacity = v; }
    public String getBusinessPackages() { return businessPackages; }
    public void setBusinessPackages(String v) { this.businessPackages = v; }
    public String getBufferDir() { return bufferDir; }
    public void setBufferDir(String v) { this.bufferDir = v; }
    public long getMaxBufferFileBytes() { return maxBufferFileBytes; }
    public void setMaxBufferFileBytes(long v) { this.maxBufferFileBytes = v; }
    public long getMaxBufferTotalBytes() { return maxBufferTotalBytes; }
    public void setMaxBufferTotalBytes(long v) { this.maxBufferTotalBytes = v; }
    public Mq getMq() { return mq; }
    public Http getHttp() { return http; }
}
