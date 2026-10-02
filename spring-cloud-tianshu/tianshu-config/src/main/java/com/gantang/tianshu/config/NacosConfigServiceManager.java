package com.gantang.tianshu.config;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * 封装 Nacos 官方 {@link ConfigService} 的生命周期、拉取与监听。
 *
 * <p>地址/命名空间/鉴权默认复用注册发现 {@code tianshu.nacos.* }，可用 {@code tianshu.nacos.config.*} 单独覆盖。
 * 仅使用官方 nacos-client，不引入任何 SCA 组件。
 *
 * @author gantang
 */
public class NacosConfigServiceManager implements DisposableBean {

    private final NacosConfigProperties properties;
    private final Environment environment;
    private volatile ConfigService configService;

    public NacosConfigServiceManager(NacosConfigProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /** 懒创建并缓存 ConfigService。 */
    public ConfigService configService() {
        if (configService == null) {
            synchronized (this) {
                if (configService == null) {
                    try {
                        configService = com.alibaba.nacos.api.NacosFactory.createConfigService(buildProperties());
                    } catch (NacosException e) {
                        throw new IllegalStateException("创建 Nacos ConfigService 失败: " + e.getMessage(), e);
                    }
                }
            }
        }
        return configService;
    }

    private Properties buildProperties() {
        Properties p = new Properties();
        p.setProperty("serverAddr", firstNonBlank(
                environment.getProperty("tianshu.nacos.config.server-addr"),
                environment.getProperty("tianshu.nacos.server-addr"),
                "127.0.0.1:8848"));
        String namespace = firstNonBlank(
                environment.getProperty("tianshu.nacos.config.namespace"),
                environment.getProperty("tianshu.nacos.namespace"),
                "");
        if (hasText(namespace)) {
            p.setProperty("namespace", namespace);
        }
        String username = firstNonBlank(
                environment.getProperty("tianshu.nacos.config.username"),
                environment.getProperty("tianshu.nacos.username"),
                "");
        if (hasText(username)) {
            p.setProperty("username", username);
            p.setProperty("password", firstNonBlank(
                    environment.getProperty("tianshu.nacos.config.password"),
                    environment.getProperty("tianshu.nacos.password"),
                    ""));
        }
        return p;
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return "";
    }

    /** 拉取配置内容；不存在返回 {@code null}。启动冷启动 gRPC 建连较慢时做有限重试。 */
    public String getConfig(String dataId, String group) {
        int attempts = 3;
        for (int i = 1; i <= attempts; i++) {
            try {
                String content = configService().getConfig(dataId, group, properties.getTimeoutMs());
                if (content != null) {
                    return content;
                }
            } catch (NacosException e) {
                if (properties.isFailFast()) {
                    throw new IllegalStateException("拉取 Nacos 配置失败 dataId=" + dataId + ": " + e.getMessage(), e);
                }
            }
            if (i < attempts) {
                sleep(1000L);
            }
        }
        return null;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 添加变更监听。 */
    public void addListener(String dataId, String group, Listener listener) {
        try {
            configService().addListener(dataId, group, listener);
        } catch (NacosException e) {
            throw new IllegalStateException("注册 Nacos 配置监听失败 dataId=" + dataId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (configService != null) {
            try {
                configService.shutDown();
            } catch (NacosException ignored) {
                // 关闭时忽略
            }
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
