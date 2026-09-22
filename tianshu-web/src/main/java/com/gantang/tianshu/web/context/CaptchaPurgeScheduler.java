package com.gantang.tianshu.web.context;

import com.gantang.tianshu.web.captcha.CaptchaStore;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时清理过期验证码，避免内存存储长期堆积。
 *
 * @author gantang
 */
public class CaptchaPurgeScheduler {

    private final CaptchaStore captchaStore;

    public CaptchaPurgeScheduler(CaptchaStore captchaStore) {
        this.captchaStore = captchaStore;
    }

    @Scheduled(fixedDelayString = "${tianshu.captcha.purge-delay:300000}")
    public void purge() {
        this.captchaStore.purgeExpired();
    }
}
