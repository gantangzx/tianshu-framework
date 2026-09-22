package com.gantang.tianshu.exchange;

import com.gantang.tianshu.exchange.common.cmd.OrderCommand;
import com.gantang.tianshu.exchange.common.config.InitialStateConfiguration;
import com.gantang.tianshu.exchange.processors.journaling.ISerializationProcessor;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

import java.io.IOException;
import java.util.function.Function;

public interface MiddlewareSerializationManager {

    boolean storeData(long snapshotId, long seq, long timestampNs, ISerializationProcessor.SerializedModuleType type, int instanceId, WriteBytesMarshallable obj);


    <T> T loadData(long snapshotId, ISerializationProcessor.SerializedModuleType type, int instanceId, Function<BytesIn<?>, T> initFunc);


    void writeToJournal(OrderCommand cmd, long dSeq, boolean eob) throws IOException;


    long replayJournalFull();
}
