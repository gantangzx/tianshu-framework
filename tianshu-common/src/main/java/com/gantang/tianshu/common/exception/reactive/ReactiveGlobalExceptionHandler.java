package com.gantang.tianshu.common.exception.reactive;

import com.gantang.tianshu.common.api.IResultCode;
import com.gantang.tianshu.common.api.R;
import com.gantang.tianshu.common.exception.ExceptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

/**
 * Reactive 栈全局异常处理器：将异常统一转换为 {@link R}。
 *
 * @author gantang
 */
@Order(-2)
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ReactiveGlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handle(ServerWebExchange exchange, Exception ex) {
        IResultCode resultCode = ExceptionSupport.resolveCode(ex);
        if (resultCode.getCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("[GlobalException] path={} 服务器异常", exchange.getRequest().getPath(), ex);
        } else {
            log.warn("[GlobalException] path={} {} - {}",
                    exchange.getRequest().getPath(), resultCode.getCode(), ex.getMessage());
        }
        R<Void> body = R.fail(resultCode, ExceptionSupport.resolveMessage(ex));
        HttpStatus status = HttpStatus.resolve(resultCode.getCode());
        return ResponseEntity.status(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
