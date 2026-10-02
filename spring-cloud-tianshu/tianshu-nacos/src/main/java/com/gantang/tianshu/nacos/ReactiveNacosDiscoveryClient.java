package com.gantang.tianshu.nacos;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * 将阻塞式 {@link NacosDiscoveryClient} 适配为 {@link ReactiveDiscoveryClient}。
 *
 * <p>阻塞查询被放到 {@link Schedulers#boundedElastic()}，避免阻塞 Netty 事件循环。
 * Gateway 的响应式 LoadBalancer 优先使用该 Bean 解析实例。
 *
 * @author gantang
 */
public class ReactiveNacosDiscoveryClient implements ReactiveDiscoveryClient {

    private final NacosDiscoveryClient delegate;

    public ReactiveNacosDiscoveryClient(NacosDiscoveryClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public String description() {
        return delegate.description() + " (reactive)";
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        return Flux.defer(() -> Flux.fromIterable(delegate.getInstances(serviceId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> getServices() {
        return Flux.defer(() -> Flux.fromIterable(delegate.getServices()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
