package com.gantang.tianshu.discovery;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 服务发现自动配置。引入本模块即具备向 Eureka 注册自身、发现其他服务的能力，
 * 无需任何注解；默认地址为本地 {@code http://localhost:8761/eureka}，
 * 生产环境通过 {@code eureka.client.service-url.defaultZone} 覆盖。
 *
 * @author gantang
 */
@AutoConfiguration
public class DiscoveryAutoConfiguration {
}
