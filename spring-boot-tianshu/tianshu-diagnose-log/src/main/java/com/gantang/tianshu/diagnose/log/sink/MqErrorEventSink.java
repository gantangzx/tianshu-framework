package com.gantang.tianshu.diagnose.log.sink;

import com.gantang.tianshu.diagnose.api.DiagnoseConstants;
import com.gantang.tianshu.diagnose.api.ErrorEvent;
import com.gantang.tianshu.mq.core.MqProducer;

/**
 * MQ 传输：复用 {@link MqProducer} 统一信封（与框架其它生产者同栈）。由单 worker 线程调用。
 */
public class MqErrorEventSink implements ErrorEventSink {

    private final MqProducer mqProducer;
    private final String topic;

    public MqErrorEventSink(MqProducer mqProducer, String topic) {
        this.mqProducer = mqProducer;
        this.topic = (topic == null || topic.isBlank()) ? DiagnoseConstants.DEFAULT_TOPIC : topic;
    }

    @Override
    public Result publish(ErrorEvent event) {
        try {
            mqProducer.send(topic, DiagnoseConstants.EVENT_TYPE, event.getFingerprint(), event);
            return Result.OK;
        } catch (RuntimeException e) {
            return Result.RETRY;
        }
    }
}
