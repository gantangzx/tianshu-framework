package com.gantang.tianshu.xxjob;

import com.gantang.tianshu.xxjob.config.XxlJobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobAutoConfiguration {

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties properties) {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(properties.getAdminAddresses());
        xxlJobSpringExecutor.setAppname(properties.getExecutorAppName());
        xxlJobSpringExecutor.setAddress(properties.getExecutorAddress());
        xxlJobSpringExecutor.setIp(properties.getExecutorIp());
        xxlJobSpringExecutor.setPort(properties.getExecutorPort());
        xxlJobSpringExecutor.setAccessToken(properties.getAccessToken());
        xxlJobSpringExecutor.setLogPath(properties.getExecutorLogPath());
        xxlJobSpringExecutor.setLogRetentionDays(properties.getExecutorLogRetentionDays());
        return xxlJobSpringExecutor;
    }

}
