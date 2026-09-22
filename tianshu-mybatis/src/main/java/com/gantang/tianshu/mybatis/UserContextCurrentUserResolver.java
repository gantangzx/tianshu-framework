package com.gantang.tianshu.mybatis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 通过反射桥接 {@code tianshu-user-context} 的用户上下文，避免硬依赖。
 *
 * <p>当类路径存在 {@code com.gantang.tianshu.user.core.UserContextHolder} 时读取当前用户 ID；
 * 否则返回 {@code null}，审计字段中用户相关列不填充。</p>
 *
 * @author gantang
 */
public class UserContextCurrentUserResolver implements CurrentUserResolver {

    private static final Logger log = LoggerFactory.getLogger(UserContextCurrentUserResolver.class);
    private static final String HOLDER_CLASS = "com.gantang.tianshu.user.core.UserContextHolder";

    private final Method getCurrentUserMethod;
    private final Method getUserIdMethod;

    public UserContextCurrentUserResolver() {
        Method holderMethod = null;
        Method userIdMethod = null;
        try {
            Class<?> holderClass = Class.forName(HOLDER_CLASS);
            holderMethod = holderClass.getMethod("getCurrentUser");
            if (holderMethod.getReturnType() != null) {
                userIdMethod = holderMethod.getReturnType().getMethod("getUserId");
            }
        } catch (ReflectiveOperationException | LinkageError ex) {
            log.debug("tianshu-user-context 不在类路径，审计用户字段将不自动填充");
        }
        this.getCurrentUserMethod = holderMethod;
        this.getUserIdMethod = userIdMethod;
    }

    @Override
    public Long currentUserId() {
        if (this.getCurrentUserMethod == null || this.getUserIdMethod == null) {
            return null;
        }
        try {
            Object user = this.getCurrentUserMethod.invoke(null);
            if (user == null) {
                return null;
            }
            Object userId = this.getUserIdMethod.invoke(user);
            return userId instanceof Long longId ? longId : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
