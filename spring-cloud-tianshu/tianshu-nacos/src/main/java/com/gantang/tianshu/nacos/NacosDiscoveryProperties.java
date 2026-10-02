package com.gantang.tianshu.nacos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos 注册发现可配置项。前缀 {@code tianshu.nacos}。
 *
 * <p>不与 SCA 的 {@code spring.cloud.nacos.* } 耦合，避免依赖未 GA 的 starter。
 *
 * @author gantang
 */
@ConfigurationProperties(prefix = "tianshu.nacos")
public class NacosDiscoveryProperties {

    /** 是否启用 Nacos 注册发现。 */
    private boolean enabled = true;

    /** Nacos 服务端地址 host:port，多个用逗号分隔。 */
    private String serverAddr = "127.0.0.1:8848";

    /** 命名空间 ID（public 留空）。 */
    private String namespace = "";

    /** 分组。 */
    private String group = "DEFAULT_GROUP";

    /** 集群名，多个用逗号分隔。 */
    private String clusterName = "DEFAULT";

    /** 用户名（未开启鉴权留空）。 */
    private String username = "";

    /** 密码。 */
    private String password = "";

    /** 是否把本服务注册到 Nacos（gateway 也可注册，默认 true）。 */
    private boolean registerEnabled = true;

    /** 实例权重。 */
    private double weight = 1.0;

    /** 是否为临时实例（临时实例由客户端心跳/长连接维持）。 */
    private boolean ephemeral = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getServerAddr() { return serverAddr; }
    public void setServerAddr(String serverAddr) { this.serverAddr = serverAddr; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isRegisterEnabled() { return registerEnabled; }
    public void setRegisterEnabled(boolean registerEnabled) { this.registerEnabled = registerEnabled; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public boolean isEphemeral() { return ephemeral; }
    public void setEphemeral(boolean ephemeral) { this.ephemeral = ephemeral; }
}
