package com.gantang.tianshu.gray;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;

/**
 * 灰度能力自动配置：将 {@link GrayLoadBalancerConfiguration} 作为所有 LoadBalancer 客户端的
 * 默认配置，引入即生效，无需逐服务声明。
 *
 * @author gantang
 */
@AutoConfiguration
@LoadBalancerClients(defaultConfiguration = GrayLoadBalancerConfiguration.class)
public class GrayAutoConfiguration {
}
