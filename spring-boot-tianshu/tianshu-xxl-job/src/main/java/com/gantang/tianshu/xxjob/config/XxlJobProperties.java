package com.gantang.tianshu.xxjob.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(XxlJobProperties.GENERATOR_PREFIX)
public class XxlJobProperties {
    private Logger logger = LoggerFactory.getLogger(XxlJobProperties.class);
    protected static final String GENERATOR_PREFIX = "xxl.job";

    private String adminAddresses;

    private String accessToken;

    private String executorAppName;

    private String executorAddress;

    private String executorIp;

    private int executorPort;

    private String executorLogPath;

    private int executorLogRetentionDays;

}
