package com.gantang.tianshu.config;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 在 {@link ConfigEnvironmentPostProcessor}（上下文创建前）与 {@code ConfigAutoConfiguration}
 * （上下文创建后）之间共享同一个 {@link NacosConfigServiceManager}，避免重复创建 ConfigService。
 *
 * <p>配置中心为进程级单例组件，使用静态持有；应用关闭时由 Spring 调用 manager 的 destroy。
 *
 * @author gantang
 */
final class NacosConfigManagers {

    private static final AtomicReference<NacosConfigServiceManager> HOLDER = new AtomicReference<>();

    private NacosConfigManagers() {
    }

    static void register(NacosConfigServiceManager manager) {
        HOLDER.set(manager);
    }

    static NacosConfigServiceManager get() {
        return HOLDER.get();
    }
}
