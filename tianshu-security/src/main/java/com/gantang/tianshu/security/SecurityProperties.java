package com.gantang.tianshu.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全组件配置属性。
 *
 * @author gantang
 */
@ConfigurationProperties(prefix = SecurityProperties.PREFIX)
public class SecurityProperties {

    public static final String PREFIX = "tianshu.security";

    /** 是否启用 JWT 安全。 */
    private boolean enabled = true;

    /** HMAC 签名密钥，生产环境必须显式配置；长度建议不少于 32 字节。 */
    private String secret = "tianshu-default-secret-please-change-in-production-0123456789";

    /** JWT 签发者。 */
    private String issuer = "tianshu";

    /** 访问令牌有效期（秒），默认 2 小时。 */
    private long accessTokenTtl = 7200;

    /** 刷新令牌有效期（秒），默认 7 天。 */
    private long refreshTokenTtl = 604800;

    /** 无需鉴权即可访问的路径（Ant 风格）。 */
    private List<String> permitAll = new ArrayList<>();

    /** 是否启用无状态 JWT（不做服务端黑名单校验）。关闭后将依赖 TokenStore 校验。 */
    private boolean stateless = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(long accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public long getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(long refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public List<String> getPermitAll() {
        return permitAll;
    }

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll;
    }

    public boolean isStateless() {
        return stateless;
    }

    public void setStateless(boolean stateless) {
        this.stateless = stateless;
    }
}
