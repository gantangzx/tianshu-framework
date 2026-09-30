package com.gantang.tianshu.diagnose.api;

/**
 * 采集端与服务端共享的传输常量。
 */
public final class DiagnoseConstants {

    /** 当前契约版本。 */
    public static final int SCHEMA_VERSION = 1;

    /** 默认 MQ destination/topic。 */
    public static final String DEFAULT_TOPIC = "error-log";

    /** 默认 HTTP 接收入口路径。 */
    public static final String DEFAULT_HTTP_PATH = "/api/v1/error-events";

    /** 信封事件类型（MQ eventType）。 */
    public static final String EVENT_TYPE = "ErrorEvent";

    private DiagnoseConstants() {
    }
}
