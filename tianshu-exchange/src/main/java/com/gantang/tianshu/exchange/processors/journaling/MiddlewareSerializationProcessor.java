package com.gantang.tianshu.exchange.processors.journaling;


import com.gantang.tianshu.exchange.ExchangeApi;
import com.gantang.tianshu.exchange.MiddlewareSerializationManager;
import com.gantang.tianshu.exchange.common.cmd.OrderCommand;
import com.gantang.tianshu.exchange.common.config.ExchangeConfiguration;
import com.gantang.tianshu.exchange.common.config.InitialStateConfiguration;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.NavigableMap;
import java.util.function.Function;


/**
 * 集群序列化日志重放功能
 */
@Slf4j
public class MiddlewareSerializationProcessor implements ISerializationProcessor {

    private final String exchangeId;

    private final long baseSeq;

    private long baseSnapshotId;

    private long enableJournalAfterSeq = -1;

    private final MiddlewareSerializationManager middlewareSerializationManager;


    public MiddlewareSerializationProcessor(ExchangeConfiguration exchangeConfig, MiddlewareSerializationManager middlewareSerializationManager) {
        this.middlewareSerializationManager = middlewareSerializationManager;
        final InitialStateConfiguration initStateCfg = exchangeConfig.getInitStateCfg();
        this.exchangeId = initStateCfg.getExchangeId();
        this.baseSnapshotId = initStateCfg.getSnapshotId();
        this.baseSeq = initStateCfg.getSnapshotBaseSeq();
    }

    @Override
    public boolean storeData(long snapshotId, long seq, long timestampNs, SerializedModuleType type, int instanceId, WriteBytesMarshallable obj) {
        return middlewareSerializationManager.storeData(snapshotId, seq, timestampNs, type, instanceId, obj);
    }

    @Override
    public <T> T loadData(long snapshotId, SerializedModuleType type, int instanceId, Function<BytesIn<?>, T> initFunc) {
        return middlewareSerializationManager.loadData(snapshotId, type, instanceId, initFunc);
    }

    @Override
    public void writeToJournal(OrderCommand cmd, long dSeq, boolean eob) throws IOException {
        middlewareSerializationManager.writeToJournal(cmd, dSeq, eob);
    }

    @Override
    public void enableJournaling(long afterSeq, ExchangeApi api) {
        enableJournalAfterSeq = afterSeq;
        api.groupingControl(0, 1);
    }

    @Override
    public NavigableMap<Long, SnapshotDescriptor> findAllSnapshotPoints() {
        return null;
    }

    @Override
    public void replayJournalStep(long snapshotId, long seqFrom, long seqTo, ExchangeApi api) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long replayJournalFull(InitialStateConfiguration initialStateConfiguration, ExchangeApi api) {
        return middlewareSerializationManager.replayJournalFull();
    }

    @Override
    public void replayJournalFullAndThenEnableJouraling(InitialStateConfiguration initialStateConfiguration, ExchangeApi exchangeApi) {
        long seq = replayJournalFull(initialStateConfiguration, exchangeApi);
        enableJournaling(seq, exchangeApi);
    }

    @Override
    public boolean checkSnapshotExists(long snapshotId, SerializedModuleType type, int instanceId) {
        return false;
    }
}
