package com.gantang.tianshu.exchange.common.config;

import com.gantang.tianshu.exchange.processors.journaling.DiskSerializationProcessor;
import com.gantang.tianshu.exchange.processors.journaling.DiskSerializationProcessorConfiguration;
import com.gantang.tianshu.exchange.processors.journaling.DummySerializationProcessor;
import com.gantang.tianshu.exchange.processors.journaling.ISerializationProcessor;
import com.gantang.tianshu.exchange.MiddlewareSerializationManager;
import com.gantang.tianshu.exchange.processors.journaling.MiddlewareSerializationProcessor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.function.Function;

@AllArgsConstructor
@Getter
@Builder
@ToString
public class SerializationConfiguration {

    // no serialization
    public static final SerializationConfiguration DEFAULT = SerializationConfiguration.builder()
            .enableJournaling(false)
            .serializationProcessorFactory(cfg -> DummySerializationProcessor.INSTANCE)
            .build();

    // no journaling, only snapshots
    public static final SerializationConfiguration DISK_SNAPSHOT_ONLY = SerializationConfiguration.builder()
            .enableJournaling(false)
            .serializationProcessorFactory(exchangeCfg -> new DiskSerializationProcessor(exchangeCfg, DiskSerializationProcessorConfiguration.createDefaultConfig()))
            .build();

    // snapshots and journaling
    public static final SerializationConfiguration DISK_JOURNALING = SerializationConfiguration.builder()
            .enableJournaling(true)
            .serializationProcessorFactory(exchangeCfg -> new DiskSerializationProcessor(exchangeCfg, DiskSerializationProcessorConfiguration.createDefaultConfig()))
            .build();

    // 自定义序列化配置
    public static SerializationConfiguration customSerializationConfiguration(boolean enableJournaling,String storageFolder) {
        return SerializationConfiguration.builder()
                .enableJournaling(enableJournaling)
                .serializationProcessorFactory(exchangeCfg -> new DiskSerializationProcessor(exchangeCfg, DiskSerializationProcessorConfiguration.createDefaultConfigWithStorageFolder(storageFolder)))
                .build();
    }

    /**
     * 中间件序列化配置
     * @param enableJournaling
     * @param middlewareSerializationManager
     * @return
     */
    public static SerializationConfiguration middlewareSerializationConfiguration(boolean enableJournaling, MiddlewareSerializationManager middlewareSerializationManager) {
        return SerializationConfiguration.builder()
                .enableJournaling(enableJournaling)
                .serializationProcessorFactory(exchangeCfg -> new MiddlewareSerializationProcessor(exchangeCfg, middlewareSerializationManager))
                .build();
    }

    /*
     * Enables journaling.
     * Set to false for analytics instances.
     */
    private final boolean enableJournaling;

    /*
     * Serialization processor implementations
     */
    private final Function<ExchangeConfiguration, ? extends ISerializationProcessor> serializationProcessorFactory;


}
