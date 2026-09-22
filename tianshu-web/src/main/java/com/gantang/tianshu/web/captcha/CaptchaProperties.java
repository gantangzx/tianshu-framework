package com.gantang.tianshu.web.captcha;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 验证码配置属性。
 *
 * @author gantang
 */
@ConfigurationProperties(prefix = CaptchaProperties.PREFIX)
public class CaptchaProperties {

    public static final String PREFIX = "tianshu.captcha";

    /** 是否启用验证码端点与服务。 */
    private boolean enabled = true;

    /** 渲染类型。 */
    private CaptchaType type = CaptchaType.LINE;

    /** 图片宽度（像素）。 */
    private int width = 200;

    /** 图片高度（像素）。 */
    private int height = 70;

    /** 干扰元素数量。 */
    private int interfereCount = 10;

    /** 字符验证码字符数（算术类型忽略）。 */
    private int codeCount = 4;

    /** 验证码有效时长（分钟）。 */
    private long expireMinutes = 5;

    /** Servlet 图片端点路径。 */
    private String servletPath = "/captcha/image";

    /** Reactive 图片端点路径。 */
    private String reactivePath = "/captcha/image";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CaptchaType getType() {
        return type;
    }

    public void setType(CaptchaType type) {
        this.type = type;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getInterfereCount() {
        return interfereCount;
    }

    public void setInterfereCount(int interfereCount) {
        this.interfereCount = interfereCount;
    }

    public int getCodeCount() {
        return codeCount;
    }

    public void setCodeCount(int codeCount) {
        this.codeCount = codeCount;
    }

    public long getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(long expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public String getReactivePath() {
        return reactivePath;
    }

    public void setReactivePath(String reactivePath) {
        this.reactivePath = reactivePath;
    }
}
