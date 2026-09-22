package com.gantang.tianshu.common.api;

/**
 * 框架内置统一状态码。
 *
 * <p>编码与 HTTP 语义保持一致；自定义业务错误码请使用 10000 ~ 19999 区间。</p>
 *
 * @author gantang
 */
public enum ResultCode implements IResultCode {

    // ---------------- 成功 ----------------

    SUCCESS(200, "操作成功"),

    // ---------------- 客户端错误 4xx ----------------

    FAILURE(400, "业务异常"),
    PARAM_MISSING(400, "缺少必要的请求参数"),
    PARAM_TYPE_ERROR(400, "请求参数类型错误"),
    PARAM_BIND_ERROR(400, "请求参数绑定错误"),
    PARAM_VALID_ERROR(400, "参数校验失败"),
    MESSAGE_NOT_READABLE(400, "请求消息不可读"),

    UNAUTHORIZED(401, "未授权或登录已失效"),
    TOKEN_INVALID(401, "Token 校验失败"),
    TOKEN_EXPIRED(401, "Token 已过期"),
    CLIENT_UNAUTHORIZED(401, "客户端未授权"),

    FORBIDDEN(403, "请求被拒绝"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_SUPPORTED(405, "不支持当前请求方法"),
    MEDIA_TYPE_NOT_SUPPORTED(415, "不支持当前媒体类型"),

    // ---------------- 自定义业务码 ----------------

    LOGIN_ERROR(10000, "用户名或密码错误"),

    // ---------------- 服务端错误 5xx ----------------

    INTERNAL_SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
