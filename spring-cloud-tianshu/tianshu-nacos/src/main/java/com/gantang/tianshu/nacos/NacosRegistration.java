package com.gantang.tianshu.nacos;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.Map;

/**
 * 描述当前应用在 Nacos 的注册信息，实现 Spring Cloud {@link Registration}。
 *
 * @author gantang
 */
public class NacosRegistration implements Registration {

    private final String serviceId;
    private final String host;
    private final int port;
    private final Map<String, String> metadata;
    private final boolean secure;

    public NacosRegistration(String serviceId, String host, int port,
                             boolean secure, Map<String, String> metadata) {
        this.serviceId = serviceId;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.metadata = metadata;
    }

    @Override
    public String getInstanceId() {
        return host + ":" + port;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public URI getUri() {
        return ServiceInstance.createUri(this);
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }
}
