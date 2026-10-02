package com.gantang.tianshu.nacos;

import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.cloud.client.ServiceInstance;
import java.net.URI;
import java.util.Map;

/**
 * 将 Nacos {@link Instance} 适配为 Spring Cloud {@link ServiceInstance}。
 *
 * @author gantang
 */
public class NacosServiceInstance implements ServiceInstance {

    private final String serviceId;
    private final Instance instance;
    private final boolean secure;

    public NacosServiceInstance(String serviceId, Instance instance, boolean secure) {
        this.serviceId = serviceId;
        this.instance = instance;
        this.secure = secure;
    }

    @Override
    public String getInstanceId() {
        String id = instance.getInstanceId();
        return id != null ? id : instance.getIp() + ":" + instance.getPort();
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getHost() {
        return instance.getIp();
    }

    @Override
    public int getPort() {
        return instance.getPort();
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
        return instance.getMetadata();
    }
}
