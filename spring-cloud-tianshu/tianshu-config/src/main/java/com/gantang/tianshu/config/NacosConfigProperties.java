package com.gantang.tianshu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Nacos 配置中心可配置项。前缀 {@code tianshu.nacos.config}。
 *
 * <p>仅基于官方 nacos-client，不依赖 SCA starter。地址默认复用 {@code tianshu.nacos.server-addr}，
 * 也可单独用 {@code tianshu.nacos.config.server-addr} 覆盖。
 *
 * @author gantang
 */
@ConfigurationProperties(prefix = "tianshu.nacos.config")
public class NacosConfigProperties {

    /** 是否启用 Nacos 配置中心。 */
    private boolean enabled = true;

    /** Nacos 配置中心地址，留空则回退到 tianshu.nacos.server-addr。 */
    private String serverAddr = "";

    /** 命名空间 ID（public 留空），默认复用 tianshu.nacos.namespace。 */
    private String namespace = "";

    /** 用户名（未开启鉴权留空），默认复用 tianshu.nacos.username。 */
    private String username = "";

    /** 密码，默认复用 tianshu.nacos.password。 */
    private String password = "";

    /** 分组。 */
    private String group = "DEFAULT_GROUP";

    /** 默认拉取文件扩展名（当 dataId 未写后缀时用于自动拼装）。 */
    private String fileExtension = "yml";

    /** 是否开启动态刷新。 */
    private boolean refreshEnabled = true;

    /** 拉取超时（毫秒）。 */
    private long timeoutMs = 5000;

    /** 额外要拉取的 dataId（除默认的 ${spring.application.name}.${file-extension} 外）。 */
    private List<String> sharedConfigs = new ArrayList<>();

    /** 配置缺失/拉取失败是否不阻断启动。 */
    private boolean failFast = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getServerAddr() { return serverAddr; }
    public void setServerAddr(String serverAddr) { this.serverAddr = serverAddr; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public boolean isRefreshEnabled() { return refreshEnabled; }
    public void setRefreshEnabled(boolean refreshEnabled) { this.refreshEnabled = refreshEnabled; }

    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

    public List<String> getSharedConfigs() { return sharedConfigs; }
    public void setSharedConfigs(List<String> sharedConfigs) { this.sharedConfigs = sharedConfigs; }

    public boolean isFailFast() { return failFast; }
    public void setFailFast(boolean failFast) { this.failFast = failFast; }
}
