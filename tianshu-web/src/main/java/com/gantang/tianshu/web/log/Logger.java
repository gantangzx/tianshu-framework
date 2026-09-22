package com.gantang.tianshu.web.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法日志注解。标注于方法上，由切面统一记录入参、耗时与异常。
 *
 * <p>Servlet 与 Reactive 双栈均生效：对 {@code Mono/Flux} 返回值，
 * 日志会挂载到异步信号完成时。</p>
 *
 * @author gantang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Logger {

    /**
     * 日志描述，留空时使用 类名#方法名。
     *
     * @return 描述文本
     */
    String value() default "";

    /**
     * 是否打印方法入参。
     *
     * @return 是否打印入参
     */
    boolean args() default true;
}
