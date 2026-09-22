package com.gantang.tianshu.user.reactive;

import com.gantang.tianshu.user.core.UserContext;
import com.gantang.tianshu.user.core.UserContextHolder;
import com.gantang.tianshu.user.core.UserContextResolver;
import com.gantang.tianshu.user.core.UserRequestSupport;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Reactive {@link WebFilter} that resolves the {@link UserContext} once per request and makes
 * it available in two complementary ways:
 *
 * <ol>
 *   <li>As a Reactor Context entry under {@link #CONTEXT_KEY}, readable inside the reactive
 *       pipeline regardless of the executing thread.</li>
 *   <li>Bound to {@link UserContextHolder} for the duration of the downstream pipeline, so
 *       blocking infrastructure (e.g. MyBatis meta-object handlers) keeps using the same
 *       accessor as on the servlet stack.</li>
 * </ol>
 */
public class ReactiveUserContextFilter implements WebFilter {

    public static final String CONTEXT_KEY = "tianshu.userContext";

    private final UserContextResolver resolver;

    public ReactiveUserContextFilter(UserContextResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Map<String, List<String>> headers =
                UserRequestSupport.toHeaderMap(request.getHeaders());
        String ip = request.getRemoteAddress() != null
                ? (request.getRemoteAddress().getAddress() != null
                        ? request.getRemoteAddress().getAddress().getHostAddress()
                        : request.getRemoteAddress().getHostString())
                : null;
        UserContext context = resolver.resolve(headers, ip);

        Mono<Void> downstream = Mono.defer(() -> {
            if (context != null) {
                UserContextHolder.setContext(context);
            }
            return chain.filter(exchange);
        }).doFinally(signal -> UserContextHolder.clear());

        return context != null
                ? downstream.contextWrite(ctx -> ctx.put(CONTEXT_KEY, context))
                : downstream;
    }
}
