package com.gantang.tianshu.user.core;

/**
 * Immutable snapshot of the authenticated principal for the current request.
 *
 * <p>Instances are produced at the web boundary (servlet filter / reactive web filter)
 * and stored in {@link UserContextHolder}. The same object model is used on both stacks,
 * so business code never has to distinguish Servlet from Reactive.</p>
 */
public record DefaultUserContext(UserDetails userDetails, String token, String ip) implements UserContext {

    @Override
    public UserDetails getUserDetail() {
        return userDetails;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getIp() {
        return ip;
    }
}
