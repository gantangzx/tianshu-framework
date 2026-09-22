package com.gantang.tianshu.sleuth;

import com.gantang.tianshu.sleuth.instrument.xxjob.TraceXxJobAspect;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

/**
 * Micrometer Tracing based auto-configuration. The Brave bridge, propagation and
 * sampling are all provided by Spring Boot's tracing auto-configuration; this class
 * only adds framework-specific instrumentation: the XXL-Job aspect and a filter that
 * echoes the current trace id in the response headers.
 */
@AutoConfiguration
public class SleuthTraceAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "com.xxl.job.core.handler.annotation.XxlJob")
    public TraceXxJobAspect traceXxJobAspect(Tracer tracer) {
        return new TraceXxJobAspect(tracer);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletTraceResponseConfiguration {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 30)
        public Filter traceIdInResponseServletFilter(Tracer tracer) {
            return (request, response, chain) -> {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null) {
                    ((HttpServletResponse) response).addHeader("x-trace-id",
                            currentSpan.context().traceId());
                }
                chain.doFilter(request, response);
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class ReactiveTraceResponseConfiguration {

        @Bean
        public WebFilter traceIdInResponseReactiveFilter(Tracer tracer) {
            return (exchange, chain) -> {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null) {
                    exchange.getResponse().getHeaders()
                            .add("x-trace-id", currentSpan.context().traceId());
                }
                return chain.filter(exchange);
            };
        }
    }
}
