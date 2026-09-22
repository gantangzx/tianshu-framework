package com.gantang.tianshu.web.context;

import com.gantang.tianshu.web.captcha.CaptchaProperties;
import com.gantang.tianshu.web.captcha.CaptchaService;
import com.gantang.tianshu.web.captcha.CaptchaStore;
import com.gantang.tianshu.web.captcha.InMemoryCaptchaStore;
import com.gantang.tianshu.web.captcha.reactive.ReactiveCaptchaController;
import com.gantang.tianshu.web.captcha.servlet.ServletCaptchaController;
import com.gantang.tianshu.web.ip.IpRegionProperties;
import com.gantang.tianshu.web.ip.IpRegionSearcher;
import com.gantang.tianshu.web.log.LoggerAspect;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Web 增强自动配置：方法日志、验证码、IP 属地解析。
 *
 * @author gantang
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties({CaptchaProperties.class, IpRegionProperties.class})
public class WebAutoConfiguration {

    // ---------------- 方法日志 ----------------

    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnMissingBean
    public LoggerAspect loggerAspect() {
        return new LoggerAspect();
    }

    // ---------------- 验证码 ----------------

    @Bean
    @ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnMissingBean(CaptchaStore.class)
    public CaptchaStore captchaStore() {
        return new InMemoryCaptchaStore();
    }

    @Bean
    @ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnMissingBean
    public CaptchaService captchaService(CaptchaProperties properties, CaptchaStore captchaStore) {
        return new CaptchaService(properties, captchaStore);
    }

    @Bean
    @ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    public ServletCaptchaController servletCaptchaController(CaptchaService captchaService) {
        return new ServletCaptchaController(captchaService);
    }

    @Bean
    @ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnClass(name = "org.springframework.web.reactive.DispatcherHandler")
    public ReactiveCaptchaController reactiveCaptchaController(CaptchaService captchaService) {
        return new ReactiveCaptchaController(captchaService);
    }

    /** 定时清理过期验证码。 */
    @Bean
    @ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public CaptchaPurgeScheduler captchaPurgeScheduler(CaptchaStore captchaStore) {
        return new CaptchaPurgeScheduler(captchaStore);
    }

    // ---------------- IP 属地 ----------------

    @Bean(destroyMethod = "close")
    @ConditionalOnClass(Searcher.class)
    @ConditionalOnProperty(prefix = IpRegionProperties.PREFIX, name = "enabled", havingValue = "true")
    public IpRegionSearcher ipRegionSearcher(IpRegionProperties properties) {
        return new IpRegionSearcher(properties);
    }
}
