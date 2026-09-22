package com.gantang.tianshu.gray;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;

/**
 * 灰度负载均衡器装配。配合 {@code @LoadBalancerClients(defaultConfiguration = ...)} 使用，
 * 对全部服务生效。
 *
 * @author gantang
 */
public class GrayLoadBalancerConfiguration {

    @Bean
    public ReactorServiceInstanceLoadBalancer grayLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
            org.springframework.core.env.Environment environment) {
        String serviceId = environment.getProperty("loadbalancer.client.name");
        return new GrayLoadBalancer(supplierProvider, serviceId);
    }
}
