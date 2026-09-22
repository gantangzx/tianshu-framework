package com.gantang.tianshu.common.exception;

import com.gantang.tianshu.common.api.IResultCode;
import com.gantang.tianshu.common.api.ResultCode;

import java.io.Serial;

/**
 * 业务异常。
 *
 * <p>用于在业务逻辑中表达可预期的失败，携带统一的 {@link IResultCode}。
 * 框架的全局异常处理器会将其转换为统一响应体，而不是落到容器默认错误页。</p>
 *
 * @author gantang
 */
public class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final IResultCode resultCode;

    public ServiceException(String message) {
        super(message);
        this.resultCode = ResultCode.FAILURE;
    }

    public ServiceException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public ServiceException(IResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ServiceException(IResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public IResultCode getResultCode() {
        return resultCode;
    }
}
