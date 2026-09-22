package com.gantang.tianshu.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关配置属性。
 *
 * @author gantang
 */
@ConfigurationProperties(prefix = GatewayProperties.PREFIX)
public class GatewayProperties {

    public static final String PREFIX = "tianshu.gateway";

    /** 是否启用 Swagger 文档聚合。 */
    private boolean docEnabled = true;

    /** 聚合文档中每个服务读取 OpenAPI 的默认下游路径。 */
    private String apiDocsPath = "/v3/api-docs";

    /** 需要从文档聚合中排除的服务名（支持大小写不敏感匹配）。 */
    private List<String> excludedServices = new ArrayList<>();

    public boolean isDocEnabled() {
        return docEnabled;
    }

    public void setDocEnabled(boolean docEnabled) {
        this.docEnabled = docEnabled;
    }

    public String getApiDocsPath() {
        return apiDocsPath;
    }

    public void setApiDocsPath(String apiDocsPath) {
        this.apiDocsPath = apiDocsPath;
    }

    public List<String> getExcludedServices() {
        return excludedServices;
    }

    public void setExcludedServices(List<String> excludedServices) {
        this.excludedServices = excludedServices;
    }
}
