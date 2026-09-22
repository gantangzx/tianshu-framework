package com.gantang.tianshu.gray;

/**
 * 灰度相关常量。
 *
 * @author gantang
 */
public final class GrayConstants {

    private GrayConstants() {
    }

    /** 灰度标记请求头 / 元数据键。 */
    public static final String GRAY_HEADER = "x-gray-tag";

    /** 灰度版本元数据键。 */
    public static final String GRAY_METADATA_KEY = "gray";

    /** 灰度版本取值。 */
    public static final String GRAY_VALUE = "true";
}
