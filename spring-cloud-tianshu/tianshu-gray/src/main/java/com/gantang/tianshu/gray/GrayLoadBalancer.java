package com.gantang.tianshu.gray;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 灰度负载均衡器：当请求携带灰度标记时，仅在灰度实例中选择；否则在非灰度实例中选择。
 *
 * <p>基于 Spring Cloud LoadBalancer 的响应式契约实现，Servlet 与 Reactive 调用均生效。</p>
 *
 * @author gantang
 */
public class GrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;

    public GrayLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> supplierProvider, String serviceId) {
        this.supplierProvider = supplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = this.supplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().map(instances -> select(instances, request));
    }

    private Response<ServiceInstance> select(List<ServiceInstance> instances, Request request) {
        if (instances.isEmpty()) {
            return new EmptyResponse();
        }
        boolean gray = isGrayRequest(request);
        List<ServiceInstance> filtered = instances.stream()
                .filter(instance -> isGrayInstance(instance) == gray)
                .toList();

        List<ServiceInstance> candidates = filtered.isEmpty() ? instances : filtered;
        ServiceInstance chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return new DefaultResponse(chosen);
    }

    private boolean isGrayRequest(Request request) {
        try {
            Object context = request.getContext();
            Object clientRequest = context.getClass().getMethod("getClientRequest").invoke(context);
            Object headers = clientRequest.getClass().getMethod("getHeaders").invoke(clientRequest);
            if (headers instanceof HttpHeaders httpHeaders) {
                return GrayConstants.GRAY_VALUE.equalsIgnoreCase(httpHeaders.getFirst(GrayConstants.GRAY_HEADER));
            }
        } catch (Exception ignored) {
            // 无法解析时按非灰度处理
        }
        return false;
    }

    private boolean isGrayInstance(ServiceInstance instance) {
        return GrayConstants.GRAY_VALUE.equalsIgnoreCase(
                instance.getMetadata().get(GrayConstants.GRAY_METADATA_KEY));
    }

    public String getServiceId() {
        return this.serviceId;
    }
}
