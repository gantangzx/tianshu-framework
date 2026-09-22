package com.gantang.tianshu.web.captcha;

/**
 * 新生成的验证码图片及其标识。
 *
 * @param key      客户端需回传的唯一标识
 * @param image    PNG/GIF 图片字节
 * @param base64   data-URI 前缀的 base64 表示
 * @param gif      是否为 GIF 动图
 * @author gantang
 */
public record CaptchaImage(String key, byte[] image, String base64, boolean gif) {
}
