package com.gantang.tianshu.user.core;

/**
 * Thread-bound access point for the current {@link UserContext}.
 *
 * <p>Both the servlet filter and the reactive filter populate this holder at the start of
 * request processing and clear it afterwards. Under WebFlux the framework propagates the
 * holder state onto the reactive worker threads (see ReactiveUserContextWebFilter), so the
 * same blocking accessor is safe for synchronous infrastructure such as MyBatis handlers.</p>
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setContext(UserContext userContext) {
        CONTEXT.set(userContext);
    }

    public static UserContext getContext() {
        return CONTEXT.get();
    }

    /**
     * @return current user details, or {@code null} when unauthenticated.
     */
    public static UserDetails getCurrentUser() {
        UserContext context = CONTEXT.get();
        return context == null ? null : context.getUserDetail();
    }

    /**
     * @return current user id, or {@code null} when unauthenticated.
     */
    public static Long getCurrentUserId() {
        UserDetails user = getCurrentUser();
        return user == null ? null : user.getUserId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
