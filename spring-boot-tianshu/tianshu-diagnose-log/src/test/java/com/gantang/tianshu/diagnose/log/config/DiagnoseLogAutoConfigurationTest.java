package com.gantang.tianshu.diagnose.log.config;

import com.gantang.tianshu.diagnose.log.sink.BufferingErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.ErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.HttpErrorEventSink;
import com.gantang.tianshu.diagnose.log.sink.LoggingErrorEventSink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnoseLogAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DiagnoseLogAutoConfiguration.class));

    @Test
    void defaultFallsBackToLoggingWhenNoMqProducer() {
        runner.run(context -> {
            ErrorEventSink sink = context.getBean(ErrorEventSink.class);
            assertThat(sink).isInstanceOf(BufferingErrorEventSink.class);
            assertThat(((BufferingErrorEventSink) sink).delegate())
                    .isInstanceOf(LoggingErrorEventSink.class);
        });
    }

    @Test
    void httpTransportSelectedWhenEndpointProvided() {
        runner.withPropertyValues(
                "tianshu.diagnose.transport=http",
                "tianshu.diagnose.http.endpoint=http://localhost:9999/error-events")
                .run(context -> {
                    ErrorEventSink sink = context.getBean(ErrorEventSink.class);
                    assertThat(((BufferingErrorEventSink) sink).delegate())
                            .isInstanceOf(HttpErrorEventSink.class);
                });
    }

    @Test
    void disabledWhenPropertyFalse() {
        runner.withPropertyValues("tianshu.diagnose.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ErrorEventSink.class));
    }
}
