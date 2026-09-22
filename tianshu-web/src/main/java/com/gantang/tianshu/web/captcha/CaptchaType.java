package com.gantang.tianshu.web.captcha;

/**
 * 验证码渲染类型。
 *
 * @author gantang
 */
public enum CaptchaType {

    /** 直线干扰。 */
    LINE,

    /** 圆圈干扰。 */
    CIRCLE,

    /** 扭曲字符。 */
    SHEAR,

    /** 动态 GIF。 */
    GIF,

    /** 算术表达式，结果为校验码。 */
    MATH
}
