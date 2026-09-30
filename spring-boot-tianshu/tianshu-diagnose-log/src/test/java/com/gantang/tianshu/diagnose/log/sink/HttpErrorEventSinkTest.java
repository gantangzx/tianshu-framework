package com.gantang.tianshu.diagnose.log.sink;

import com.gantang.tianshu.diagnose.api.ErrorEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HttpErrorEventSinkTest {

    private HttpServer server;
    private int status = 202;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/error-events", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] empty = new byte[0];
            exchange.sendResponseHeaders(status, empty.length);
            exchange.getResponseBody().write(empty);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private ErrorEvent event() {
        ErrorEvent e = new ErrorEvent();
        e.setMsgId("m1");
        e.setFingerprint("f1");
        return e;
    }

    @Test
    void successReturnsOk() {
        String url = "http://localhost:" + server.getAddress().getPort() + "/error-events";
        assertThat(new HttpErrorEventSink(url, Duration.ofSeconds(2)).publish(event()).isSuccess()).isTrue();
    }

    @Test
    void serverErrorIsRetryable() {
        status = 503;
        String url = "http://localhost:" + server.getAddress().getPort() + "/error-events";
        ErrorEventSink.Result r = new HttpErrorEventSink(url, Duration.ofSeconds(2)).publish(event());
        assertThat(r.isRetryable()).isTrue();
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void clientErrorDropsWithoutRetry() {
        status = 400;
        String url = "http://localhost:" + server.getAddress().getPort() + "/error-events";
        ErrorEventSink.Result r = new HttpErrorEventSink(url, Duration.ofSeconds(2)).publish(event());
        assertThat(r.isRetryable()).isFalse();
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void unreachableEndpointIsRetryable() {
        ErrorEventSink.Result r = new HttpErrorEventSink(
                "http://127.0.0.1:1/error-events", Duration.ofSeconds(1)).publish(event());
        assertThat(r.isRetryable()).isTrue();
    }
}
