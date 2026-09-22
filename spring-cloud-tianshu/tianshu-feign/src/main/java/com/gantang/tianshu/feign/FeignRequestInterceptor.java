package com.gantang.tianshu.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * OpenFeign 请求拦截器：在 Servlet 调用链中把上游的关键请求头透传给下游服务，
 * 实现鉴权、链路追踪与灰度标记在服务间的自动传递。
 *
 * @author gantang
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    /** 默认透传的请求头。 */
    static final List<String> PROPAGATED_HEADERS = Arrays.asList(
            "authorization",
            "x-request-id",
            "x-b3-traceid",
            "x-b3-spanid",
            "x-gray-tag",
            "traceparent",
            "tracestate");

    @Override
    public void apply(RequestTemplate template) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }
        for (String header : PROPAGATED_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !template.headers().containsKey(header)) {
                template.header(header, value);
            }
        }
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }
}
