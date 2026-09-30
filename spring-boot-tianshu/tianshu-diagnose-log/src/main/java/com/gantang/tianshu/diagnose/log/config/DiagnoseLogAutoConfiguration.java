package com.gantang.tianshu.diagnose.log.config;

import com.gantang.tianshu.diagnose.log.sink.BufferingErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.ErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.HttpErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.LoggingErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.MqErrorEventSink;
import com.gantang.tianshu.mq.core.MqProducer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.nio.file.Paths;
import java.time.Duration;

/**
 * 采集 starter 自动装配。MQ/HTTP 传输按类与属性条件装配；选定传输外包一层缓冲；
 * 通过 {@link ApplicationRunner} 在 logback 初始化完成后延迟绑定。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "tianshu.diagnose", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DiagnoseLogProperties.class)
public class DiagnoseLogAutoConfiguration {

    /**
     * 按所选传输 + 类可用性构造主 sink（已包缓冲）。永远可装配：最差退化为 logging 兜底。
     */
    @Bean
    @ConditionalOnMissingBean(ErrorEventSink.class)
    public ErrorEventSink diagnoseSink(DiagnoseLogProperties props,
                                       org.springframework.beans.factory.ObjectProvider<MqProducer> mqProducer) {
        ErrorEventSink primary = selectPrimary(props, mqProducer);
        return new BufferingErrorEventSink(primary, Paths.get(props.getBufferDir()),
                props.getMaxBufferFileBytes(), props.getMaxBufferTotalBytes());
    }

    private ErrorEventSink selectPrimary(DiagnoseLogProperties props,
                                         org.springframework.beans.factory.ObjectProvider<MqProducer> mqProducer) {
        String transport = props.getTransport() == null ? "mq" : props.getTransport().trim().toLowerCase();
        switch (transport) {
            case "mq": {
                MqProducer producer = mqProducer.getIfAvailable();
                if (producer != null) {
                    return new MqErrorEventSink(producer, props.getMq().getTopic());
                }
                break;
            }
            case "http": {
                String endpoint = props.getHttp().getEndpoint();
                if (endpoint != null && !endpoint.isBlank()) {
                    Duration timeout = Duration.ofMillis(
                            Math.max(props.getHttp().getConnectTimeoutMs(), props.getHttp().getReadTimeoutMs()));
                    return new HttpErrorEventSink(endpoint, timeout);
                }
                break;
            }
            case "logging":
                return new LoggingErrorEventSink();
            default:
                break;
        }
        return new LoggingErrorEventSink();
    }

    @Bean
    public ApplicationRunner diagnoseAppenderBinder(ErrorEventSink sink,
                                                     DiagnoseLogProperties props,
                                                     Environment environment) {
        return args -> new ErrorLogAppenderBinder(props, environment).bind(sink);
    }
}
