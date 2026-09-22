package com.gantang.tianshu.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 基于 Nimbus JOSE + JWT（HMAC-SHA256）的令牌服务，自包含、无需密钥库。
 *
 * @author gantang
 */
public class TokenService {

    private static final String ROLES_CLAIM = "roles";

    private final SecurityProperties properties;
    private final MACSigner signer;
    private final MACVerifier verifier;

    public TokenService(SecurityProperties properties) {
        this.properties = properties;
        try {
            this.signer = new MACSigner(properties.getSecret());
            this.verifier = new MACVerifier(properties.getSecret());
        } catch (JOSEException ex) {
            throw new IllegalStateException("初始化 JWT 签名器失败，请检查 tianshu.security.secret 长度", ex);
        }
    }

    /**
     * 为登录用户签发令牌对。
     *
     * @param user 登录用户
     * @return 令牌对
     */
    public TokenPair issue(LoginUser user) {
        Instant now = Instant.now();
        String accessToken = sign(user, now, this.properties.getAccessTokenTtl(), "access");
        String refreshToken = sign(user, now, this.properties.getRefreshTokenTtl(), "refresh");
        return new TokenPair(accessToken, refreshToken, this.properties.getAccessTokenTtl());
    }

    /**
     * 校验并解析令牌。
     *
     * @param token JWT 字符串
     * @return 解析出的登录用户；签名无效或已过期返回空
     */
    public Optional<LoginUser> parse(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(this.verifier)) {
                return Optional.empty();
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                return Optional.empty();
            }
            List<String> roles = claims.getStringListClaim(ROLES_CLAIM);
            return Optional.of(new LoginUser(claims.getSubject(),
                    claims.getStringClaim("username"), null,
                    roles == null ? List.of() : roles));
        } catch (ParseException | JOSEException ex) {
            return Optional.empty();
        }
    }

    private String sign(LoginUser user, Instant now, long ttlSeconds, String type) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(this.properties.getIssuer())
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim(ROLES_CLAIM, user.getRoles())
                .claim("type", type)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJWT.sign(this.signer);
        } catch (JOSEException ex) {
            throw new IllegalStateException("签发令牌失败", ex);
        }
        return signedJWT.serialize();
    }
}
