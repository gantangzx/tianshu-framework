package com.gantang.tianshu.gateway.doc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gantang.tianshu.gateway.GatewayProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

/**
 * Swagger / OpenAPI 聚合端点。
 *
 * <ul>
 *     <li>{@code /v3/api-docs} 返回聚合文档（合并各服务的 paths/tags，重写 servers 指向网关路由）。</li>
 *     <li>{@code /swagger/services} 返回服务清单，供自定义前端或 swagger-ui url 配置使用。</li>
 *     <li>{@code /v3/api-docs/{service}} 透传指定服务的原始 OpenAPI（经网关路由）。</li>
 * </ul>
 *
 * @author gantang
 */
@RestController
@RequestMapping
public class SwaggerAggregationController {

    private final ReactiveDiscoveryClient discoveryClient;
    private final WebClient webClient;
    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;

    public SwaggerAggregationController(ReactiveDiscoveryClient discoveryClient, WebClient.Builder webClientBuilder,
                                        GatewayProperties properties, ObjectMapper objectMapper) {
        this.discoveryClient = discoveryClient;
        this.webClient = webClientBuilder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 服务文档清单。
     *
     * @return 文档清单流
     */
    @GetMapping(value = "/swagger/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ServiceDoc> services() {
        return eligibleServices()
                .map(name -> new ServiceDoc(name, "/v3/api-docs/" + name));
    }

    /**
     * 聚合所有服务的 OpenAPI。
     *
     * @return 聚合后的 OpenAPI JSON
     */
    @GetMapping(value = "/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ObjectNode> aggregate() {
        return eligibleServices()
                .flatMap(this::fetchServiceDoc)
                .collectList()
                .map(this::merge);
    }

    /**
     * 透传单个服务的 OpenAPI。
     *
     * @param service 服务名
     * @return 该服务 OpenAPI JSON
     */
    @GetMapping(value = "/v3/api-docs/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ObjectNode> single(@PathVariable String service) {
        return fetchServiceDoc(service);
    }

    private Flux<String> eligibleServices() {
        List<String> excluded = this.properties.getExcludedServices().stream()
                .map(s -> s.toLowerCase(Locale.ROOT)).toList();
        return this.discoveryClient.getServices()
                .filter(name -> !excluded.contains(name.toLowerCase(Locale.ROOT)));
    }

    private Mono<ObjectNode> fetchServiceDoc(String service) {
        return this.discoveryClient.getInstances(service).next().flatMap(instance -> {
            String url = instanceUri(instance) + this.properties.getApiDocsPath();
            return this.webClient.get().uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(body -> rewriteServers(body, service))
                    .onErrorResume(ex -> Mono.empty());
        });
    }

    private String instanceUri(ServiceInstance instance) {
        String scheme = instance.isSecure() ? "https" : "http";
        return scheme + "://" + instance.getHost() + ":" + instance.getPort();
    }

    private ObjectNode rewriteServers(String body, String service) {
        try {
            ObjectNode node = (ObjectNode) this.objectMapper.readTree(body);
            ObjectNode server = objectMapper.createObjectNode();
            server.put("url", "/" + service);
            server.put("description", service);
            node.set("servers", objectMapper.createArrayNode().add(server));
            return node;
        } catch (Exception ex) {
            return null;
        }
    }

    private ObjectNode merge(List<ObjectNode> docs) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.1");
        root.set("info", objectMapper.createObjectNode()
                .put("title", "Tianshu Gateway Aggregated API")
                .put("version", "1.0"));
        root.set("servers", objectMapper.createArrayNode());

        ObjectNode paths = objectMapper.createObjectNode();
        java.util.LinkedHashSet<com.fasterxml.jackson.databind.JsonNode> tags = new java.util.LinkedHashSet<>();
        for (ObjectNode doc : docs) {
            if (doc == null) {
                continue;
            }
            if (doc.has("paths")) {
                doc.get("paths").fields().forEachRemaining(entry -> paths.set(entry.getKey(), entry.getValue()));
            }
            if (doc.has("tags")) {
                doc.get("tags").forEach(tags::add);
            }
        }
        root.set("paths", paths);
        root.set("tags", objectMapper.valueToTree(tags));
        return root;
    }
}
