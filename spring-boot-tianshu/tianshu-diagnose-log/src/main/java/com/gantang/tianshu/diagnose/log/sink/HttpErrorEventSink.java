package com.gantang.tianshu.diagnose.log.sink;

import com.gantang.tianshu.diagnose.api.ErrorEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP 传输：JDK HttpClient POST JSON。5xx/IO 异常 → RETRY（缓冲）；4xx → DROP（不重试）。
 * 由单 worker 线程调用，可安全复用 HttpClient。
 */
public class HttpErrorEventSink implements ErrorEventSink {

    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;

    public HttpErrorEventSink(String endpoint, Duration timeout) {
        this.endpoint = URI.create(endpoint);
        this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Result publish(ErrorEvent event) {
        try {
            String body = ErrorEventJson.toJson(event);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return Result.OK;
            }
            if (code >= 500) {
                return Result.RETRY;
            }
            return Result.DROP;
        } catch (Exception e) {
            return Result.RETRY;
        }
    }
}
