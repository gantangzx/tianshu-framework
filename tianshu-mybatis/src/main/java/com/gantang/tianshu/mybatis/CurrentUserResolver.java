package com.gantang.tianshu.mybatis;

/**
 * 当前用户 ID 解析策略。默认实现尝试反射读取 {@code tianshu-user-context}，
 * 业务系统也可提供自定义 Bean（如从 SecurityContext 读取）覆盖。
 *
 * @author gantang
 */
@FunctionalInterface
public interface CurrentUserResolver {

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID，未登录或无法获取时返回 {@code null}
     */
    Long currentUserId();
}
