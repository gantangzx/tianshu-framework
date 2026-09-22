package com.gantang.tianshu.web.captcha;

/**
 * 验证码存储抽象。应用可提供自定义实现（如 Redis）覆盖默认内存实现。
 *
 * @author gantang
 */
public interface CaptchaStore {

    /**
     * 保存验证码。
     *
     * @param key   唯一标识
     * @param entry 验证码条目
     */
    void put(String key, CaptchaEntry entry);

    /**
     * 查找并移除指定验证码。
     *
     * @param key 唯一标识
     * @return 验证码条目，不存在返回 {@code null}
     */
    CaptchaEntry take(String key);

    /**
     * 清除所有已过期条目。
     */
    void purgeExpired();
}
