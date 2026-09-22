package com.gantang.tianshu.registry;

import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Configuration;

/**
 * Eureka 注册中心自动配置。引入本模块即启用 Eureka Server，无需再手写
 * {@code @EnableEurekaServer}。
 *
 * <p>典型用法：新建一个只含依赖与少量配置的 Spring Boot 应用即可作为注册中心。</p>
 *
 * @author gantang
 */
@Configuration
@EnableEurekaServer
public class RegistryAutoConfiguration {
}
