package com.gantang.tianshu.nacos;

import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Nacos 的 Spring Cloud {@link DiscoveryClient} 实现。
 *
 * <p>Gateway 的 {@code lb://} 路由与 OpenFeign 的负载均衡均通过该抽象取实例，
 * 因此复用这一桥接即可，无需改造 Gateway/Feign。
 *
 * @author gantang
 */
public class NacosDiscoveryClient implements DiscoveryClient {

    public static final String DESCRIPTION = "Spring Cloud Nacos Discovery Client (official nacos-client)";

    private final NacosServiceManager manager;

    public NacosDiscoveryClient(NacosServiceManager manager) {
        this.manager = manager;
    }

    @Override
    public String description() {
        return DESCRIPTION;
    }

    @Override
    public List<org.springframework.cloud.client.ServiceInstance> getInstances(String serviceId) {
        List<Instance> instances = manager.getAllInstances(serviceId);
        List<org.springframework.cloud.client.ServiceInstance> result = new ArrayList<>(instances.size());
        for (Instance instance : instances) {
            result.add(new NacosServiceInstance(serviceId, instance, false));
        }
        return result;
    }

    @Override
    public List<String> getServices() {
        return manager.getServices();
    }
}
