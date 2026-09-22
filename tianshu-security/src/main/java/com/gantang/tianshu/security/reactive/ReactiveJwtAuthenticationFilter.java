package com.gantang.tianshu.security.reactive;

import com.gantang.tianshu.security.LoginUser;
import com.gantang.tianshu.security.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive JWT 认证过滤器：修复旧实现响应式失效的问题，正确通过 ReactiveSecurityContextHolder 传播。
 *
 * @author gantang
 */
public class ReactiveJwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public ReactiveJwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = resolveToken(exchange);
        if (token == null) {
            return chain.filter(exchange);
        }
        return this.tokenService.parse(token)
                .map(user -> authenticate(user))
                .map(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .orElseGet(() -> chain.filter(exchange));
    }

    private UsernamePasswordAuthenticationToken authenticate(LoginUser user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private String resolveToken(ServerWebExchange exchange) {
        List<String> headers = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String header = headers.get(0);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
