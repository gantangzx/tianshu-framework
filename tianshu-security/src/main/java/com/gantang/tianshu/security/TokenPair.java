package com.gantang.tianshu.security;

/**
 * 访问令牌与刷新令牌对。
 *
 * @param accessToken  访问令牌
 * @param refreshToken 刷新令牌
 * @param expiresIn    访问令牌有效期（秒）
 * @author gantang
 */
public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
}
