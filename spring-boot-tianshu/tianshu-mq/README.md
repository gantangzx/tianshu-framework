# tianshu-mq

基于 **Spring Cloud Stream** 的消息队列封装，是 tianshu-framework 的 Boot 生态组件之一。

- 统一消息信封 `MqMessage<T>`，禁止业务方另造平行消息体
- 生产端 `MqProducer`：基于 `StreamBridge` 动态发送，自动注入 msgId / source / traceId
- 消费端 `AbstractMqConsumer<T>`：统一类型转换、幂等钩子、异常收口
- traceId 自动透传（消息头 `x-trace-id` + 信封字段），与 `spring-cloud-tianshu-sleuth` 风格一致；无 tracing 依赖时自动降级
- 不绑定具体中间件：RocketMQ / Kafka / RabbitMQ 仅替换 binder 依赖，业务代码零改动

> MQ 为非 Web 组件，不涉及 servlet / reactive 双栈（与 tianshu-xxl-job、tianshu-elasticsearch 同理）。

## 版本与坐标

版本统一在 `tianshu-dependencies-bom` 收口，业务侧引入时不写版本：

```xml
<dependency>
    <groupId>com.gantang</groupId>
    <artifactId>tianshu-mq</artifactId>
</dependency>
```

对应版本关系（实测对齐）：Spring Cloud `2025.1.0`、Spring Cloud Alibaba `2025.1.0.0`、RocketMQ 客户端 `5.3.1`。

## 引入 binder

Starter 不强制传递具体 binder，业务方按需引入。

**RocketMQ（默认推荐，对齐 Nacos 生态）：**

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rocketmq</artifactId>
</dependency>
```

**Kafka / RabbitMQ：** 换成 `spring-cloud-stream-binder-kafka` / `spring-amqp` 即可，下文业务代码无需改动。

## 配置示例

```yaml
spring:
  application:
    name: order-service
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:8087
      bindings:
        # 生产端：destination 即 topic
        orderEvent-out-0:
          destination: order-topic
          content-type: application/json
        # 消费端：binding 名 = 函数 bean 名 + -in-0
        orderCreatedConsumer-in-0:
          destination: order-topic
          group: order-consumer-group
          content-type: application/json

# 框架增强能力（均有默认值，可整段省略）
tianshu:
  mq:
    enabled: true              # 总开关，默认 true
    envelope-enabled: true     # 是否包装统一信封，默认 true（对接历史 topic 可关）
    trace-enabled: true        # 是否透传 traceId，默认 true
    source: order-service      # 缺省取 spring.application.name
```

## 生产消息

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MqProducer mqProducer;

    public void createOrder(Order order) {
        // ... 落库逻辑
        mqProducer.send("orderEvent", "order-created", order.getOrderNo(),
                new OrderCreatedEvent(order.getOrderNo(), order.getAmount()));
    }
}
```

`send` 重载：

- `send(destination, payload)`：事件类型默认取 destination
- `send(destination, eventType, payload)`
- `send(destination, eventType, bizKey, payload)`
- `send(destination, MqMessage)`：发送自行构造的信封

## 消费消息

继承 `AbstractMqConsumer<T>`，声明为函数式 Bean，**Bean 名即函数名**：

```java
@Configuration
@RequiredArgsConstructor
public class MqConsumerConfig {

    private final OrderProcessService orderProcessService;

    @Bean
    public AbstractMqConsumer<OrderCreatedEvent> orderCreatedConsumer(
            ObjectMapper objectMapper, TracePropagator tracePropagator) {
        return new AbstractMqConsumer<>(objectMapper, tracePropagator) {
            @Override
            protected Class<OrderCreatedEvent> getPayloadType() {
                return OrderCreatedEvent.class;
            }

            @Override
            protected boolean isDuplicate(MqMessage<OrderCreatedEvent> message) {
                // 基于 msgId / bizKey 查消费记录，建议落唯一索引或 Redis SETNX
                return orderProcessService.isProcessed(message.getMsgId());
            }

            @Override
            protected void handle(MqMessage<OrderCreatedEvent> message) {
                orderProcessService.process(message.getPayload());
            }
        };
    }
}
```

对应 binding：`orderCreatedConsumer-in-0`。

## 设计约定

1. **幂等**：跨服务写消息的消费端必须幂等。优先基于 `msgId`（框架自动生成的全局唯一 ID）或业务唯一键去重，消费记录建议落库唯一索引。
2. **重试与死信**：消费异常直接上抛，由 binder 自身的 retry / DLQ 机制处理，框架不另造重试轮子，避免双重重试语义。
3. **最终一致**：跨服务写操作遵循"本地事务 + 消息 + 消费端幂等"的最终一致模式；需要事务消息时使用 RocketMQ 原生事务消息能力。
4. **历史 topic 对接**：对端不识别信封时，置 `tianshu.mq.envelope-enabled=false`，生产/消费均按裸 payload 处理。

## 装配条件

`MqAutoConfiguration` 仅在以下条件全部满足时生效：

- 类路径存在 `org.springframework.cloud.stream.function.StreamBridge`
- `tianshu.mq.enabled` 未置为 `false`

因此未引入 spring-cloud-stream 的工程不会触发任何装配，也不会因缺失 binder 报错。
