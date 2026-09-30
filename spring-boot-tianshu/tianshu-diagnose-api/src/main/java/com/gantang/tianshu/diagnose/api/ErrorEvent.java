package com.gantang.tianshu.diagnose.api;

import java.util.List;

/**
 * 错误事件 JSON 契约。采集端（tianshu-diagnose-log）与智能体服务（tianshu-diagnosis-server）
 * 唯一共享的数据结构。
 *
 * <p>刻意不使用任何专有注解/类型：普通 POJO + getter/setter，保证 Jackson 2 与 Jackson 3
 * 均可按字段名直接（反）序列化。新增字段只能向后兼容，并提升 {@link DiagnoseConstants#SCHEMA_VERSION}。
 */
public class ErrorEvent {

    /** 契约版本号，当前 {@link DiagnoseConstants#SCHEMA_VERSION}。 */
    private int schemaVersion;

    /** 事件唯一 ID（UUID 去横线），幂等键。 */
    private String msgId;

    private String traceId;
    private String appName;
    private String env;
    private String host;

    private String level;
    private String logger;

    /** 仅业务异常显式暴露 resultCode 时取，否则为空。 */
    private String errorCode;

    private String exceptionClass;
    private String message;

    /** 堆栈帧（默认前 N 帧）。 */
    private List<String> stackFrames;

    private String fingerprint;

    /** epoch millis。 */
    private long timestamp;

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getStackFrames() {
        return stackFrames;
    }

    public void setStackFrames(List<String> stackFrames) {
        this.stackFrames = stackFrames;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
