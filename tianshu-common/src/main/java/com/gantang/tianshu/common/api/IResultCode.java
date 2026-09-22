package com.gantang.tianshu.common.api;

import java.io.Serializable;

/**
 * 业务状态码契约。
 *
 * <p>框架内置实现见 {@link ResultCode}；业务系统可实现该接口自定义状态码，
 * 建议自定义业务错误码使用 10000 ~ 19999 区间，避免与 HTTP 语义码冲突。</p>
 *
 * @author gantang
 */
public interface IResultCode extends Serializable {

    /**
     * 获取状态码。
     *
     * @return 状态码
     */
    int getCode();

    /**
     * 获取提示信息。
     *
     * @return 提示信息
     */
    String getMessage();
}
