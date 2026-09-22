package com.gantang.tianshu.user.core;

import java.util.List;
import java.util.Map;

/**
 * Strategy for resolving a {@link UserContext} from the raw information carried by an
 * incoming request, independent of the underlying web stack.
 *
 * <p>Implementations typically read an authorization token from the headers and decode it.
 * The same SPI is invoked by both the servlet and the reactive web filter.</p>
 */
@FunctionalInterface
public interface UserContextResolver {

    /**
     * @param headers a case-insensitive (multi) map of request headers
     * @param remoteIp the resolved client IP address
     * @return the resolved user context, or {@code null} if no user can be resolved
     */
    UserContext resolve(Map<String, List<String>> headers, String remoteIp);
}
