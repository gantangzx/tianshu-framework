package com.gantang.tianshu.web.captcha;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link ConcurrentHashMap} 的默认内存存储，适合单实例部署。
 *
 * @author gantang
 */
public class InMemoryCaptchaStore implements CaptchaStore {

    private final Map<String, CaptchaEntry> entries = new ConcurrentHashMap<>();

    @Override
    public void put(String key, CaptchaEntry entry) {
        this.entries.put(key, entry);
    }

    @Override
    public CaptchaEntry take(String key) {
        CaptchaEntry entry = this.entries.remove(key);
        if (entry != null && entry.isExpired()) {
            return null;
        }
        return entry;
    }

    @Override
    public void purgeExpired() {
        this.entries.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
