package com.gantang.tianshu.nacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用启动完成后自动注册到 Nacos，关闭时自动注销。
 *
 * <p>采用 {@link SmartLifecycle}，在 Web 服务器/上下文就绪后执行，对 MVC 与 WebFlux(Gateway) 均适用。
 * 注册失败仅记录错误（可配置 fail-fast），不阻断业务上下文。
 *
 * @author gantang
 */
public class NacosAutoServiceRegistration implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(NacosAutoServiceRegistration.class);

    private final NacosDiscoveryProperties properties;
    private final NacosServiceManager manager;
    private final Environment environment;

    private volatile boolean running = false;
    private volatile NacosRegistration registration;

    public NacosAutoServiceRegistration(NacosDiscoveryProperties properties,
                                        NacosServiceManager manager,
                                        Environment environment) {
        this.properties = properties;
        this.manager = manager;
        this.environment = environment;
    }

    @Override
    public void start() {
        if (!properties.isRegisterEnabled()) {
            log.info("[tianshu-nacos] register-enabled=false, skip registration");
            return;
        }
        try {
            registration = buildRegistration();
            manager.register(registration);
            running = true;
            log.info("[tianshu-nacos] registered {} to Nacos {} (group={})",
                    registration.getInstanceId(), properties.getServerAddr(), properties.getGroup());
        } catch (RuntimeException e) {
            log.error("[tianshu-nacos] register failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void stop() {
        if (registration != null) {
            try {
                manager.deregister(registration);
                log.info("[tianshu-nacos] deregistered {} from Nacos", registration.getInstanceId());
            } catch (RuntimeException e) {
                log.warn("[tianshu-nacos] deregister failed: {}", e.getMessage());
            }
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // 较晚启动、较早停止，确保地址端口已确定
        return Integer.MAX_VALUE - 100;
    }

    private NacosRegistration buildRegistration() {
        String serviceId = environment.getProperty("spring.application.name", "application");
        int port = resolvePort();
        String host = resolveHost();
        boolean secure = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("preserved.register.source", "SPRING_CLOUD");
        return new NacosRegistration(serviceId, host, port, secure, metadata);
    }

    private int resolvePort() {
        Integer port = environment.getProperty("server.port", Integer.class);
        if (port != null && port > 0) {
            return port;
        }
        // WebFlux gateway 默认 8080
        return 8080;
    }

    private String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
