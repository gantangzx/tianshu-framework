package com.gantang.tianshu.user.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.List;
import java.util.Map;

/**
 * {@link UserContextResolver} that reads the {@code uid} and {@code username} claims from a
 * bearer JWT.
 *
 * <p>Signature verification is intentionally not performed here: it is the responsibility of
 * the Spring Security resource server that runs ahead of this filter chain. This resolver only
 * extracts the principal for population into {@link UserContextHolder}. When a
 * {@link JWSVerifier} is supplied the token is additionally verified and rejected on failure.</p>
 */
public class JwtUserContextResolver implements UserContextResolver {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWSVerifier verifier;

    public JwtUserContextResolver() {
        this(null);
    }

    public JwtUserContextResolver(JWSVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public UserContext resolve(Map<String, List<String>> headers, String remoteIp) {
        String token = extractToken(headers);
        if (token == null) {
            return null;
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (verifier != null && !jwt.verify(verifier)) {
                return null;
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Object uidClaim = claims.getClaim(UserContext.UID);
            if (uidClaim == null) {
                return null;
            }
            long uid = Long.parseLong(uidClaim.toString());
            String username = claims.getClaim("username") == null
                    ? null : claims.getClaim("username").toString();

            UserDetails user = new User(uid, username, null, null);
            return new DefaultUserContext(user, token, remoteIp);
        } catch (JOSEException | java.text.ParseException | NumberFormatException e) {
            return null;
        }
    }

    private static String extractToken(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (AUTHORIZATION.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value != null && !value.isBlank()) {
                        String trimmed = value.trim();
                        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                            return trimmed.substring(BEARER_PREFIX.length()).trim();
                        }
                        return trimmed;
                    }
                }
            }
        }
        return null;
    }
}
