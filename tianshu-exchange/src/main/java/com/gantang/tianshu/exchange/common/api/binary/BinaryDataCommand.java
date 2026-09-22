package com.gantang.tianshu.exchange.common.api.binary;

import net.openhft.chronicle.bytes.WriteBytesMarshallable;

public interface BinaryDataCommand extends WriteBytesMarshallable {

    int getBinaryCommandTypeCode();

}
