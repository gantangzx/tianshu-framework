package com.gantang.tianshu.web.ip;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IP 属地解析配置属性。
 *
 * @author gantang
 */
@ConfigurationProperties(prefix = IpRegionProperties.PREFIX)
public class IpRegionProperties {

    public static final String PREFIX = "tianshu.ip2region";

    /** 是否启用 IP 属地解析。 */
    private boolean enabled = false;

    /** classpath 下的 xdb 数据文件路径。 */
    private String dbFile = "ipdb/ip2region.xdb";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDbFile() {
        return dbFile;
    }

    public void setDbFile(String dbFile) {
        this.dbFile = dbFile;
    }
}
