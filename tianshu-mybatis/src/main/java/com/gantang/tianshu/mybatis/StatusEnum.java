package com.gantang.tianshu.mybatis;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 通用业务状态枚举。
 *
 * @author gantang
 */
public enum StatusEnum {

    /** 正常。 */
    NORMAL(1, "正常"),

    /** 停用。 */
    DISABLED(0, "停用");

    @EnumValue
    private final int code;
    private final String desc;

    StatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }
}
