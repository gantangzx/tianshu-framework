package com.gantang.tianshu.utils;

import com.gantang.tianshu.constants.StringPool;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Servlet-only web helpers (Jakarta Servlet API). Reactive code must not use this class; use
 * the stack-independent helpers in {@code tianshu-user-context} or operate directly on the
 * {@code ServerWebExchange}.
 */
public class WebUtil extends WebUtils {

    private static final String[] IP_HEADER_NAMES = new String[]{
            "x-forwarded-for",
            "Proxy-Client-IP",
            "WL-Proxy-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private static final Predicate<String> IP_PREDICATE =
            (ip) -> StrUtil.isBlank(ip) || StringPool.UNKNOWN.equalsIgnoreCase(ip);

    private WebUtil() {
    }

    @Nullable
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        return requestAttributes instanceof ServletRequestAttributes servletAttributes
                ? servletAttributes.getRequest()
                : null;
    }

    /**
     * Resolve the client IP for a servlet request.
     *
     * @param request the current servlet request
     * @return the resolved IP, or {@code null} when unavailable
     */
    @Nullable
    public static String getIP(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = null;
        for (String ipHeader : IP_HEADER_NAMES) {
            ip = request.getHeader(ipHeader);
            if (!IP_PREDICATE.test(ip)) {
                break;
            }
        }
        if (IP_PREDICATE.test(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StrUtil.isBlank(ip)) {
            return null;
        }
        return Func.splitTrim(ip, StringPool.COMMA)[0];
    }

    /**
     * Write a JSON body directly to a servlet response (used by security exception handlers).
     */
    public static void response(HttpServletResponse response, String content) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(content);
    }
}
