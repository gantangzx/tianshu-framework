package com.gantang.tianshu.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Nacos 配置中心自动配置。引入本模块即具备从 Nacos 拉取配置、动态刷新的能力，
 * 无需任何注解；默认地址为本地 {@code 127.0.0.1:8848}，默认 dataId 为
 * {@code ${spring.application.name}.yml}，且配置缺失不阻断启动。
 * 生产环境通过 {@code spring.cloud.nacos.config.server-addr} 覆盖地址。
 *
 * @author gantang
 */
@AutoConfiguration
public class ConfigAutoConfiguration {
}
