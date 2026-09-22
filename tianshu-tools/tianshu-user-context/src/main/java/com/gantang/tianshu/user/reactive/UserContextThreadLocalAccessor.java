package com.gantang.tianshu.user.reactive;

import com.gantang.tianshu.user.core.UserContext;
import com.gantang.tianshu.user.core.UserContextHolder;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * Bridges {@link UserContextHolder}'s ThreadLocal with the Reactor Context so the current
 * {@link UserContext} is transparently restored on whatever thread processes the pipeline.
 *
 * <p>The accessor key matches {@link ReactiveUserContextFilter#CONTEXT_KEY}; Reactor's
 * context-propagation runtime writes the value stored under that key into this accessor on
 * thread boundaries and clears it afterwards.</p>
 */
public class UserContextThreadLocalAccessor implements ThreadLocalAccessor<UserContext> {

    @Override
    public Object key() {
        return ReactiveUserContextFilter.CONTEXT_KEY;
    }

    @Override
    public UserContext getValue() {
        return UserContextHolder.getContext();
    }

    @Override
    public void setValue(UserContext value) {
        UserContextHolder.setContext(value);
    }
}
