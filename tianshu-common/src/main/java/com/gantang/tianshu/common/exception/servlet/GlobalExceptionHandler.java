package com.gantang.tianshu.common.exception.servlet;

import com.gantang.tianshu.common.api.IResultCode;
import com.gantang.tianshu.common.api.R;
import com.gantang.tianshu.common.exception.ExceptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Servlet 栈全局异常处理器：将异常统一转换为 {@link R}。
 *
 * @author gantang
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handle(Exception ex) {
        IResultCode resultCode = ExceptionSupport.resolveCode(ex);
        if (resultCode.getCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("[GlobalException] 服务器异常", ex);
        } else {
            log.warn("[GlobalException] {} - {}", resultCode.getCode(), ex.getMessage());
        }
        R<Void> body = R.fail(resultCode, ExceptionSupport.resolveMessage(ex));
        return ResponseEntity.status(HttpStatus.resolve(resultCode.getCode()) != null
                ? HttpStatus.resolve(resultCode.getCode())
                : HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
