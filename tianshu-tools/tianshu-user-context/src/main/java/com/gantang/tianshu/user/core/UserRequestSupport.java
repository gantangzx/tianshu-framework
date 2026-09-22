package com.gantang.tianshu.user.core;

import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stack-independent helpers for turning an incoming request into the inputs of a
 * {@link UserContextResolver}.
 */
public final class UserRequestSupport {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };
    private static final String UNKNOWN = "unknown";

    private UserRequestSupport() {
    }

    /**
     * Convert Spring {@link HttpHeaders} into the multi-map expected by
     * {@link UserContextResolver}. Keys are kept as received (typically HTTP header casing).
     */
    public static Map<String, List<String>> toHeaderMap(HttpHeaders headers) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, values) -> map.put(name, new ArrayList<>(values)));
        }
        return map;
    }

    /**
     * Extract a bearer token from the {@code Authorization} header, supporting both
     * {@code Bearer xxx} and a raw token value.
     */
    public static String extractToken(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    /**
     * Best-effort resolution of the client IP from proxy headers, falling back to the
     * supplied socket address.
     */
    public static String resolveIp(HttpHeaders headers, String socketIp) {
        if (headers != null) {
            for (String header : IP_HEADERS) {
                String value = headers.getFirst(header);
                if (value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value)) {
                    int comma = value.indexOf(',');
                    return comma > 0 ? value.substring(0, comma).trim() : value.trim();
                }
            }
        }
        return socketIp;
    }
}
