package com.gantang.tianshu.user.servlet;

import com.gantang.tianshu.user.core.UserContext;
import com.gantang.tianshu.user.core.UserContextHolder;
import com.gantang.tianshu.user.core.UserContextResolver;
import com.gantang.tianshu.user.core.UserRequestSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Servlet filter that resolves the current {@link UserContext} once per request and binds it
 * to {@link UserContextHolder}. The binding is always cleared after the request completes.
 */
public class ServletUserContextFilter extends OncePerRequestFilter {

    private final UserContextResolver resolver;

    public ServletUserContextFilter(UserContextResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            HttpHeaders headers = new ServletHeaderAdapter(request);
            String ip = UserRequestSupport.resolveIp(headers, request.getRemoteAddr());
            Map<String, List<String>> headerMap = UserRequestSupport.toHeaderMap(headers);
            UserContext context = resolver.resolve(headerMap, ip);
            if (context != null) {
                UserContextHolder.setContext(context);
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
