package com.gantang.tianshu.diagnose.log.sink;

import com.gantang.tianshu.diagnose.api.ErrorEvent;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link ErrorEvent} 的 JSON 序列化，使用 Jackson 3（与业务侧 Boot 4.2 同栈）。
 */
public final class ErrorEventJson {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private ErrorEventJson() {
    }

    public static String toJson(ErrorEvent event) {
        return MAPPER.writeValueAsString(event);
    }
}
