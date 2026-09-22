package com.gantang.tianshu.common.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 统一响应结果（Result）。
 *
 * <p>推荐使用静态工厂构造，避免在业务代码中出现 {@code new}：</p>
 * <ul>
 *     <li>成功：{@link #ok()}、{@link #data(Object)}、{@link #data(Object, String)}</li>
 *     <li>失败：{@link #fail()}、{@link #fail(String)}、{@link #fail(IResultCode)}、{@link #fail(int, String)}</li>
 *     <li>布尔结果：{@link #status(boolean)}</li>
 * </ul>
 *
 * <p>{@code success} 字段仅由 {@code code} 派生，不提供独立 setter，保证状态始终一致。</p>
 *
 * @param <T> 业务数据类型
 * @author gantang
 */
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String OK_MSG = "操作成功";
    private static final String FAIL_MSG = "操作失败";

    /** 业务状态码，{@link ResultCode#SUCCESS} 表示成功。 */
    private int code;

    /** 提示信息。 */
    private String msg;

    /** 业务数据。 */
    private T data;

    private R(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    // ---------------- 成功 ----------------

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), null, OK_MSG);
    }

    public static <T> R<T> ok(String msg) {
        return new R<>(ResultCode.SUCCESS.getCode(), null, msg);
    }

    public static <T> R<T> data(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), data, OK_MSG);
    }

    public static <T> R<T> data(T data, String msg) {
        return new R<>(ResultCode.SUCCESS.getCode(), data, msg);
    }

    // ---------------- 失败 ----------------

    public static <T> R<T> fail() {
        return new R<>(ResultCode.FAILURE.getCode(), null, FAIL_MSG);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.FAILURE.getCode(), null, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, null, msg);
    }

    public static <T> R<T> fail(IResultCode resultCode) {
        return new R<>(resultCode.getCode(), null, resultCode.getMessage());
    }

    public static <T> R<T> fail(IResultCode resultCode, String msg) {
        return new R<>(resultCode.getCode(), null, msg);
    }

    /**
     * 根据布尔操作结果返回成功或失败。
     *
     * @param success 操作是否成功
     * @return 成功响应或通用失败响应
     */
    public static R<Boolean> status(boolean success) {
        return success
                ? new R<>(ResultCode.SUCCESS.getCode(), Boolean.TRUE, OK_MSG)
                : new R<>(ResultCode.FAILURE.getCode(), Boolean.FALSE, FAIL_MSG);
    }

    /**
     * 判断给定响应是否成功，{@code null} 视为失败。
     *
     * @param result 响应结果
     * @return 是否为成功响应
     */
    public static boolean isOk(R<?> result) {
        return result != null && Objects.equals(ResultCode.SUCCESS.getCode(), result.code);
    }

    public boolean success() {
        return ResultCode.SUCCESS.getCode() == this.code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
