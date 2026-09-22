package com.gantang.tianshu.common.exception;

import com.gantang.tianshu.common.api.IResultCode;
import com.gantang.tianshu.common.api.ResultCode;

import java.util.stream.Collectors;

/**
 * 异常到统一状态码/提示信息的解析逻辑，供 Servlet 与 Reactive 全局异常处理器复用。
 *
 * @author gantang
 */
public final class ExceptionSupport {

    private ExceptionSupport() {
    }

    /**
     * 将异常解析为状态码。
     *
     * @param ex 异常
     * @return 对应的状态码
     */
    public static IResultCode resolveCode(Throwable ex) {
        if (ex instanceof ServiceException serviceException) {
            return serviceException.getResultCode();
        }
        if (ex instanceof IllegalArgumentException) {
            return ResultCode.PARAM_TYPE_ERROR;
        }
        String type = ex.getClass().getName();
        return switch (type) {
            case "org.springframework.web.bind.MethodArgumentNotValidException",
                 "org.springframework.web.bind.support.WebExchangeBindException" -> ResultCode.PARAM_VALID_ERROR;
            case "org.springframework.validation.BindException" -> ResultCode.PARAM_BIND_ERROR;
            case "org.springframework.http.converter.HttpMessageNotReadableException" -> ResultCode.MESSAGE_NOT_READABLE;
            case "org.springframework.web.method.annotation.MethodArgumentTypeMismatchException",
                 "org.springframework.web.server.MissingRequestValueException" -> ResultCode.PARAM_TYPE_ERROR;
            case "org.springframework.web.method.annotation.MissingServletRequestParameterException" ->
                    ResultCode.PARAM_MISSING;
            case "org.springframework.web.HttpRequestMethodNotSupportedException" -> ResultCode.METHOD_NOT_SUPPORTED;
            case "org.springframework.http.HttpMediaTypeNotSupportedException" -> ResultCode.MEDIA_TYPE_NOT_SUPPORTED;
            case "org.springframework.web.servlet.NoHandlerFoundException" -> ResultCode.NOT_FOUND;
            default -> ResultCode.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * 提取对前端友好的错误提示。
     *
     * @param ex 异常
     * @return 提示信息
     */
    public static String resolveMessage(Throwable ex) {
        if (ex instanceof ServiceException serviceException) {
            return serviceException.getMessage();
        }
        if (ex instanceof org.springframework.web.bind.MethodArgumentNotValidException validException) {
            return joinFieldErrors(validException.getBindingResult());
        }
        if (ex instanceof org.springframework.validation.BindException bindException) {
            return joinFieldErrors(bindException.getBindingResult());
        }
        if (ex instanceof org.springframework.web.bind.support.WebExchangeBindException exchangeBind) {
            return joinFieldErrors(exchangeBind.getBindingResult());
        }
        if (ex.getMessage() != null) {
            return ex.getMessage();
        }
        return ResultCode.INTERNAL_SERVER_ERROR.getMessage();
    }

    private static String joinFieldErrors(org.springframework.validation.BindingResult bindingResult) {
        String detail = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return detail.isEmpty() ? ResultCode.PARAM_VALID_ERROR.getMessage() : detail;
    }
}
