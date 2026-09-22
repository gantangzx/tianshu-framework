package com.gantang.tianshu.common.context;

import com.gantang.tianshu.common.exception.reactive.ReactiveGlobalExceptionHandler;
import com.gantang.tianshu.common.exception.servlet.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Import;

/**
 * 公共组件自动配置：按运行栈装配对应的全局异常处理器。
 *
 * @author gantang
 */
@AutoConfiguration
@Import({CommonAutoConfiguration.ServletExceptionAdvice.class, CommonAutoConfiguration.ReactiveExceptionAdvice.class})
public class CommonAutoConfiguration {

    /** Servlet 栈异常处理器，仅当 Servlet API 存在且 WebFlux 不存在时生效。 */
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    @ConditionalOnMissingClass("org.springframework.web.reactive.HandlerResult")
    static class ServletExceptionAdvice {

        @org.springframework.context.annotation.Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }

    /** Reactive 栈异常处理器，仅当 WebFlux 存在时生效。 */
    @ConditionalOnClass(name = "org.springframework.web.reactive.HandlerResult")
    static class ReactiveExceptionAdvice {

        @org.springframework.context.annotation.Bean
        ReactiveGlobalExceptionHandler reactiveGlobalExceptionHandler() {
            return new ReactiveGlobalExceptionHandler();
        }
    }
}
