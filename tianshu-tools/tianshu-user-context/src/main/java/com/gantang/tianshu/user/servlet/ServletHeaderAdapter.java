package com.gantang.tianshu.user.servlet;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Collections;
import java.util.Enumeration;

/**
 * Read-only {@link HttpHeaders} view backed by a servlet {@link HttpServletRequest}, so the
 * stack-independent support classes can access headers without depending on the Servlet API.
 */
final class ServletHeaderAdapter extends HttpHeaders {

    ServletHeaderAdapter(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            for (String name : Collections.list(names)) {
                Enumeration<String> values = request.getHeaders(name);
                if (values != null) {
                    for (String value : Collections.list(values)) {
                        add(name, value);
                    }
                }
            }
        }
    }
}
