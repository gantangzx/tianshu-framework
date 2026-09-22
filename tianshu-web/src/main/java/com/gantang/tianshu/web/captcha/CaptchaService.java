package com.gantang.tianshu.web.captcha;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ICaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;

import java.util.concurrent.TimeUnit;

/**
 * 验证码核心服务：基于 Hutool 生成图片，校验码存入 {@link CaptchaStore}。
 *
 * @author gantang
 */
public class CaptchaService {

    private final CaptchaProperties properties;
    private final CaptchaStore store;

    public CaptchaService(CaptchaProperties properties, CaptchaStore store) {
        this.properties = properties;
        this.store = store;
    }

    /**
     * 生成验证码、存储校验码并返回编码图片。
     *
     * @return 验证码图片
     */
    public CaptchaImage generate() {
        ICaptcha captcha = createCaptcha();
        String key = IdUtil.fastSimpleUUID();
        long expireAt = System.currentTimeMillis()
                + TimeUnit.MINUTES.toMillis(this.properties.getExpireMinutes());
        this.store.put(key, new CaptchaEntry(captcha.getCode(), expireAt));

        byte[] image;
        String base64;
        if (captcha instanceof AbstractCaptcha abstractCaptcha) {
            image = abstractCaptcha.getImageBytes();
            base64 = abstractCaptcha.getImageBase64Data();
        } else {
            image = new byte[0];
            base64 = null;
        }
        return new CaptchaImage(key, image, base64, this.properties.getType() == CaptchaType.GIF);
    }

    /**
     * 一次性校验：无论成功失败都会移除该验证码。
     *
     * @param key  生成时下发的标识
     * @param code 用户输入的校验码
     * @return 标识存在、未过期且校验码匹配时返回 {@code true}
     */
    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        CaptchaEntry entry = this.store.take(key);
        return entry != null && entry.code().equalsIgnoreCase(code.trim());
    }

    private ICaptcha createCaptcha() {
        int width = this.properties.getWidth();
        int height = this.properties.getHeight();
        int interfere = this.properties.getInterfereCount();

        return switch (this.properties.getType()) {
            case CIRCLE -> CaptchaUtil.createCircleCaptcha(width, height,
                    new RandomGenerator(this.properties.getCodeCount()), interfere);
            case SHEAR -> CaptchaUtil.createShearCaptcha(width, height,
                    new RandomGenerator(this.properties.getCodeCount()), interfere);
            case GIF -> CaptchaUtil.createGifCaptcha(width, height,
                    new RandomGenerator(this.properties.getCodeCount()), interfere);
            case MATH -> CaptchaUtil.createLineCaptcha(width, height,
                    new MathGenerator(), interfere);
            case LINE -> CaptchaUtil.createLineCaptcha(width, height,
                    new RandomGenerator(this.properties.getCodeCount()), interfere);
        };
    }
}
