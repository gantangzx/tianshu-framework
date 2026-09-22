package com.gantang.tianshu.common.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Servlet 响应写出工具，供安全过滤器等无法使用 {@code @RestControllerAdvice} 的场景直接输出 JSON。
 *
 * @author gantang
 */
public final class ServletWebSupport {

    private ServletWebSupport() {
    }

    /**
     * 向响应中写出 JSON 内容。
     *
     * @param response HTTP 响应
     * @param status   HTTP 状态码
     * @param json     JSON 字符串
     * @throws IOException 写出失败时抛出
     */
    public static void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
            writer.flush();
        }
    }
}
