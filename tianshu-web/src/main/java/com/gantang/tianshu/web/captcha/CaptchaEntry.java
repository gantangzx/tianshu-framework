package com.gantang.tianshu.web.captcha;

/**
 * 存储的验证码及其过期时间戳。
 *
 * @param code     预期校验码
 * @param expireAt 过期时间（epoch millis）
 * @author gantang
 */
public record CaptchaEntry(String code, long expireAt) {

    /**
     * @return 相对当前时间是否已过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > this.expireAt;
    }
}
