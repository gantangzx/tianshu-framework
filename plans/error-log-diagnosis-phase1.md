# 实施 Plan · 错误日志采集与诊断一期（tianshu-log + 最小诊断消费）

> 目标工程：`D:\workspace\tianshu-framework`（Spring Boot 4.2.0-M1，Java 25，Servlet+Reactive 双栈）
> 模式：直接模式（有 git、无 gh），就地编辑 + 提交到 `main`
> 创建日期：2026-09-30
> 范围：仅一期。目标是让框架/业务的 ERROR 异常**零代码侵入**地进入管道：采集 → 去重指纹 → 落 ES → 简单告警。
> 二期（LLM 根因）、三期（向量知识库 RAG）、四期（MCP 主动诊断）不在本文件，仅在末尾留接口约定。
>
> 修订记录（2026-09-30，对抗评审后 v2）：
> - 生产侧与消费侧解耦：消费 Bean 默认关闭（`tianshu.log.consumer-enabled=false`），只发不收的业务方不会被迫启动消费者。
> - 消费侧改为继承现成 `AbstractMqConsumer<ErrorEvent>`（统一 Jackson3 `tools.jackson.*` + convertValue + trace + 幂等钩子），不另写平行 Consumer。
> - 补齐 spring-cloud-stream 消费必配项（destination/group/content-type），进入 starter 默认配置、README、冒烟清单。
> - Step 1 补测试基建依赖（工程当前无任何 test 依赖/用例）。
> - 自采集回路防护、缓冲仅在 worker 线程 + 字节上限、聚合计数用 ES painless upsert 保障幂等、自动配置 `after` 顺序。

## 0. 现状核实（已完成，作为设计依据）

已真实存在、一期直接复用的件：

- 双栈异常处理：
  - `tianshu-common/src/main/java/com/gantang/tianshu/common/exception/servlet/GlobalExceptionHandler.java`
    （`@RestControllerAdvice`，5xx 时 `log.error("[GlobalException] 服务器异常", ex)` 带完整堆栈）
  - `tianshu-common/.../exception/reactive/ReactiveGlobalExceptionHandler.java`
- 方法切面：`tianshu-web/src/main/java/com/gantang/tianshu/web/log/LoggerAspect.java`
  （异常分支 `log.error(..., ex)`；不依赖 Servlet API，双栈通用）
- MQ：`spring-boot-tianshu/tianshu-mq`，发送入口 `MqProducer.send(destination, eventType, bizKey, payload)`，
  底层 `StreamBridge`，统一信封 `MqMessage`，自动带 traceId；具体 binder（RocketMQ）由业务方按需引入。
- ES：`spring-boot-tianshu/tianshu-elasticsearch`，`ElasticsearchService.index(index,id,doc)` /
  `bulkIndex` / `search`，封装官方 `ElasticsearchClient`。
- 高性能交换（可选用于缓冲）：`tianshu-exchange`（Disruptor）。
- BOM：`tianshu-dependencies-bom/pom.xml` 是唯一版本源；内部模块坐标在其 `dependencyManagement`。

结论：**无需改动双栈异常处理器与 LoggerAspect 的任何 Java 代码**。日志在 web 层之下，通过一个
Logback `Appender` 即可双栈通用地截获。

---

## 目标交付物（一期完成定义 DoD）

1. 新模块 `tianshu-log`（位于 `spring-boot-tianshu/tianshu-log`），打包为可被业务方引入的 starter。
2. 业务方仅需「引依赖 + 配 MQ/ES +（可选）logback 挂 Appender」即可让 ERROR 异常进入管道，**不改一行业务代码**。
3. 自定义 Logback Appender：只拦 ERROR（可选含带异常的 WARN）、输出结构化 JSON、异步发送 + 本地磁盘缓冲（宕机/MQ 不可用不丢、不阻塞业务线程）、发送前统一脱敏。
4. 错误事件契约 `ErrorEvent` + 异常指纹算法；经 `MqProducer` 发到可配置 destination（默认 `error-log`）。
5. 最小诊断消费侧（同一 starter 内或独立包，见步骤 5）：消费 `error-log` → 按指纹聚合（同指纹只留一条 + 计数/首末时间/影响 trace）→ 原始事件落 ES（index 默认 `error-log-*`）→ 命中阈值的简单告警。
6. 版本进 BOM，`mvn clean install` 通过，关键单测覆盖指纹/脱敏/聚合。

非目标（一期不做）：LLM 根因分析、向量库、自动改代码/重启、前端页面。

---

## 架构与数据流（一期）

```
业务系统（引 tianshu-log）
  GlobalExceptionHandler / ReactiveGlobalExceptionHandler / LoggerAspect / 业务代码
        │ log.error(msg, ex)
        ▼
  Logback: <root>  ──►  ErrorLogAppender（tianshu-log 提供）
        │  ① 仅 ERROR（可配 includeWarnWithThrowable）
        │  ② 组装 ErrorEvent（结构化，含指纹、stackTopN）
        │  ③ SecretMasker 脱敏
        │  ④ 异步：有界队列 → 单独 worker；溢出/下游不可用 → 本地缓冲文件，恢复后重放
        ▼
  MqProducer.send("error-log", "ErrorEvent", fingerprint, errorEvent)   ← 复用 tianshu-mq
        │  MQ topic（binder 由业务方提供，RocketMQ/Kafka 均可）
        ▼
  ErrorLogConsumer（@Bean Consumer<Message<?>>，function 绑定 error-log-consumer）
        │  ① 指纹聚合 ErrorAggregator（同指纹合并：count++/首末时间/trace 列表）
        │  ② 原始事件写 ES：ElasticsearchService.index("error-log-yyyy.MM.dd", msgId, event)
        │  ③ 阈值简单告警（同指纹 count ≥ N / 时间窗口失败激增）→ 日志告警 + 可选 MQ 告警 destination
        ▼
  ES index（原始检索，为二期 LLM 取证与 trace 时间线预留）
```

设计红线：
- Appender 在 Logback 配置阶段可能**早于 Spring 容器就绪**，因此 Appender 与发送器必须支持
  「延迟绑定 Spring Bean」：启动期事件先进本地缓冲，容器就绪后由 `ApplicationReadyEvent` 触发绑定与 flush。
- 任何下游故障都只能「缓冲 + 丢弃计数 + 告警」，**绝不允许把异常抛回业务线程或拖慢 log.error**。
- 脱敏在 Appender 内对 message / stack / mdc 统一处理，避免手机号、身份证、卡号、SQL 参数、密钥进入 MQ/ES。

---

## 步骤拆分（一步一 PR / 一提交，按序执行）

> 每步均含：上下文简述、任务、文件落点、验证、退出标准。新 agent 可冷启动执行任意一步。

### Step 1 — 骨架：新建 `tianshu-log` 模块并接入构建与 BOM

**依赖边**：无（最先做）。

任务：
1. 新建目录 `spring-boot-tianshu/tianshu-log/`，新增 `pom.xml`：
   - parent 为 `com.gantang:spring-boot-tianshu:1.0.0-SNAPSHOT`；artifactId `tianshu-log`。
   - 依赖（版本均由 BOM 管）：
     - `com.gantang:tianshu-common`、`com.gantang:tianshu-mq`
     - `com.gantang:tianshu-elasticsearch`（**optional**：消费落库侧按 `@ConditionalOnClass/@ConditionalOnBean` 装配）
     - `org.slf4j:slf4j-api`、`ch.qos.logback:logback-classic`（provided/optional，业务方运行时自带）
     - JSON 统一用 **Jackson 3**：`tools.jackson.core:jackson-databind`（Boot 4.2 BOM 已管理；
       与 `tianshu-mq` 的 `AbstractMqConsumer` 同一栈，避免双 JSON 栈）
     - `org.springframework:spring-context`、`org.springframework.boot:spring-boot-autoconfigure`、
       `spring-cloud-stream`（provided，函数注册环境由业务方提供）
     - `spring-boot-configuration-processor`(optional)
   - **测试依赖（工程当前零测试基建，必须在本模块补齐）**：
     - `org.springframework.boot:spring-boot-starter-test`(scope test，带 JUnit5/AssertJ/reactor-test)
     - `ch.qos.logback:logback-classic`(scope test，配合编程式 `LoggerContext` 挂载 Appender)
   - mq 为生产/消费共用基础（非 optional）；elasticsearch 仅 optional。
2. `spring-boot-tianshu/pom.xml` 的 `<modules>` 增加 `<module>tianshu-log</module>`。
3. 根/聚合无需改（根 pom 已聚合 `spring-boot-tianshu`）。
4. BOM `tianshu-dependencies-bom/pom.xml`：在 starter 区（`tianshu-mq` 旁）增加
   `com.gantang:tianshu-log` 坐标，version `${project.version}`。

文件落点：
- 新增 `spring-boot-tianshu/tianshu-log/pom.xml`
- 改 `spring-boot-tianshu/pom.xml`
- 改 `tianshu-dependencies-bom/pom.xml`

验证：
```powershell
$env:JAVA_HOME='<本机JDK25>'
# 在工程根
mvn -B -pl spring-boot-tianshu/tianshu-log -am clean install -DskipTests
```

退出标准：模块可独立 install，空模块被 BOM 与聚合识别；不破坏其它模块。

---

### Step 2 — 契约：`ErrorEvent`、指纹与脱敏（纯逻辑，可单测，零 Spring）

**依赖边**：依赖 Step 1。

任务：在包 `com.gantang.tianshu.log.event` 下新增（POJO + Lombok，便于 Jackson/Logback 场景使用）：

1. `ErrorEvent`（字段，与方案一致）：
   - `String msgId`（UUID 去横线，与 `MqMessage` 风格一致，幂等去重用）
   - `String traceId`（MDC / TracePropagator；无则空）
   - `String appName`、`String env`、`String host`
   - `String level`、`String logger`
   - `String errorCode`（**仅当异常实例是业务 `ServiceException` 且暴露 resultCode 时取码**；
     Appender 拿不到 `ExceptionSupport.resolveCode`，不可用其规则映射；取不到留空）
   - `String exceptionClass`、`String message`
   - `List<String> stackFrames`（默认前 N=20 帧）
   - `String fingerprint`
   - `long timestamp`（epoch millis）

   说明：`appName/env/host` 在 `append()` 时点（Spring 未就绪）可能不可得——
   Appender 用静态默认值兜底（appName 可由 logback 属性传入；host 在 Appender `start()` 时
   解析并缓存一次，避免 `InetAddress` 反向 DNS 阻塞）；Spring 就绪后由 Step 4 回填 appName/env，
   已缓冲的历史事件不强制回填（以事件内实际值为准）。env 多 profile 时由 `Environment.getActiveProfiles()` 取。
2. `ErrorFingerprinter`：
   - 算法：`md5_hex( exceptionClass + "|" + firstBusinessFrame + "|" + errorCode )`
   - `firstBusinessFrame` = 堆栈中**第一个业务包**帧（前缀可配，默认 `com.gantang`），排除
     `java.* / jdk.* / sun.* / reactor.* / org.springframework.* / org.apache.*` 等中间件帧；
     找不到业务帧时退化为「首个非 JDK 帧」。
   - 无异常（纯 ERROR 文本）时：`md5_hex(logger + "|" + normalize(message))`，
     `normalize` 去掉数字/UUID/空白差异，避免带 ID 的同类消息产生不同指纹。
3. `ErrorSecretMasker`：**工程内无现成脱敏工具**（已全仓核实，`tianshu-mq`/common 均无
   mask 类），在此新建，不重复造轮子。至少覆盖：手机号、身份证、银行卡、邮箱、常见密钥前缀
   （`sk-`、`ark-`、`password=`、`token=`）、SQL 字面量。提供 `mask(String)`。

文件落点：
- `.../tianshu/log/event/ErrorEvent.java`
- `.../tianshu/log/event/ErrorFingerprinter.java`
- `.../tianshu/log/mask/ErrorSecretMasker.java`

验证（单测）：
- 同异常不同对象 ID → 指纹相同；不同业务帧/不同异常类 → 指纹不同。
- 业务帧缺失时退化逻辑正确；纯文本消息归一化正确。
- 脱敏：手机号/身份证/邮箱/密钥被替换，原文不落出。

退出标准：纯 JUnit 单测通过；这些类不 import 任何 `org.springframework` 与 logback（Fingerprinter/Masker）。
（`ErrorEvent` 可保持纯 POJO。）

---

### Step 3 — 采集：`ErrorLogAppender`（Logback Appender，结构化 + 异步 + 本地缓冲）

**依赖边**：依赖 Step 2。**核心且风险最高**，分配最强模型/最多评审。

任务：包 `com.gantang.tianshu.log.appender`。

1. `ErrorLogAppender extends ch.qos.logback.core.AppenderBase<ch.qos.classic.spi.ILoggingEvent>`：
   - 可配属性（logback XML `<appender>` 内 setter）：`includeWarnWithThrowable`(默认 false)、
     `stackTopN`(默认 20)、`businessPackages`(默认 `com.gantang`)、`queueCapacity`(默认 8192)、
     `bufferDir`(默认 `./.tianshu-log-buffer`)、`maxBufferFileBytes`(单文件，默认 64MB)、
     `maxBufferTotalBytes`(总缓冲，默认 512MB)。
   - `append()` 必须极轻且**只做内存操作**：过滤级别 → 构造 `ErrorEvent`（含指纹）→ 脱敏 →
     `queue.offer(event)` 即返回。offer 失败（队列满）仅递增 `dropped` 计数，
     **绝不在业务线程做文件 IO / 阻塞 / 抛异常**。
   - **自采集回路防护（必须双保险）**：
     ① logger 名以 `com.gantang.tianshu.log` 开头（Appender/sink/worker/告警自身）一律跳过；
     ② 维护一个 `ThreadLocal<Boolean> reentry`，worker 线程内标记，防止经由其它 logger 的间接重入。
     告警日志使用独立 logger（如 `com.gantang.tianshu.log.ALERT`）且不挂本 Appender。
2. 发送抽象 `ErrorEventSink`（接口：`boolean offer(ErrorEvent)`）+ 两实现：
   - `MqErrorEventSink`：持 `MqProducer`，调
     `send(destination, "ErrorEvent", fingerprint, event)`；destination 默认 `error-log`，可配。
   - `BufferingErrorEventSink`（装饰器）：**仅在 worker 线程**，下游失败/未就绪时把事件 JSONL
     追加到本地缓冲文件（Jackson3 序列化），受 `maxBufferFileBytes/maxBufferTotalBytes` 约束，
     超限丢弃最旧/当前并计 dropped；下游恢复后重放并清理，缓冲 IO 异常只 warn 不外抛。
3. 异步执行：`java.util.concurrent` 单线程 worker + 有界 `ArrayBlockingQueue`/`LinkedBlockingQueue`，
   从队列取事件 → sink.offer；线程设 daemon + named（如 `tianshu-log-sender`）。
   **不引入 tianshu-exchange/Disruptor**（其 Ignite/Chronicle/Agrona 重依赖代价过大，未被 spring-boot-tianshu 聚合）。
4. 生命周期：`start()` 启 worker（此时 Spring 未就绪，sink = 仅缓冲模式，host 在此一次性解析缓存）；
   `stop()` 触发 flush + 关线程（关闭期发送失败不再重试，避免拖住停机）。

文件落点：
- `.../appender/ErrorLogAppender.java`
- `.../appender/ErrorEventSink.java`
- `.../appender/MqErrorEventSink.java`
- `.../appender/BufferingErrorEventSink.java`

验证：
- 单测：用 `LoggerContext` 编程式挂载 Appender，触发 `log.error("x", new NPE())`，
  用一个 fake sink 断言收到的 `ErrorEvent` 字段/指纹正确；高频灌入（如 10 万条）不抛异常、
  队列满时 dropped 计数增长、业务线程耗时不显著（用多线程在业务侧验证业务线程不做 IO）。
- 自采集测试：sink/worker/告警自身产生的日志不会再被投递（计数为 0）。
- 缓冲测试：sink 持续失败时仅在 worker 线程缓冲文件增长；达到字节上限后 dropped 增长、文件不超限；
  恢复后重放成功并清理。

退出标准：`append()` 路径无任何可向调用方抛出的受检/非受检异常、无文件 IO；下游全挂也不影响主流程。

---

### Step 4 — 装配：自动配置 + 配置属性 + Appender 与 Spring 的延迟绑定

**依赖边**：依赖 Step 3。

任务：包 `com.gantang.tianshu.log`。

1. `LogErrorProperties`（`@ConfigurationProperties("tianshu.log")`）字段：
   - `enabled`(默认 true)、`destination`(默认 `error-log`)、`includeWarnWithThrowable`、
     `stackTopN`、`businessPackages`、`queueCapacity`、`bufferDir`、
     `maxBufferFileBytes`、`maxBufferTotalBytes`、
   - `consumerEnabled`(**默认 false**：只有显式开启才装配消费 Bean；只发不收的业务方不会被迫
     启动 spring-cloud-stream consumer)、`consumerGroup`、`alertThreshold`(默认 20)、
     `alertDestination`(缺省空=不转发)。
   - `appName`(缺省取 `spring.application.name`)、`env`(用 `Environment.getActiveProfiles()`，
     多 profile 逗号拼接)。
2. `LogErrorAutoConfiguration`（`@AutoConfiguration(after = MqAutoConfiguration.class)`）：
   - `@ConditionalOnProperty(prefix="tianshu.log", name="enabled", matchIfMissing=true)`
   - `@EnableConfigurationProperties(LogErrorProperties.class)`
   - 暴露 `ErrorEventSink mqErrorEventSink(MqProducer, props)`（`@ConditionalOnClass(StreamBridge)` +
     `@ConditionalOnBean(MqProducer)`）。
   - `AppenderBinder`：监听 `ApplicationReadyEvent`，遍历 Logback `LoggerContext` 找到已配置的
     `ErrorLogAppender`（按类型/名），把 Spring 管理的 sink 注入并回填 appName/env、触发缓冲 flush；
     **找不到任何已配置 Appender 时，仅 debug 日志说明（属正常：业务方可能未挂 include），不报错**。
     容器关闭时（`ContextClosedEvent`）调用 appender.stop()。
     —— 解决「Appender 早于容器」「属性双份（logback vs yml）」问题：Appender 默认值兜底，
     Spring 就绪后以 `tianshu.log.*` 为准覆盖。
3. 注册自动配置：
   `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   写入 `com.gantang.tianshu.log.LogErrorAutoConfiguration`。
4. 提供默认 logback 片段：`src/main/resources/includes/error-appender.xml`
   （声明 appender 并 attach 到 root），业务方在 **logback-spring.xml** 中 `<include>`，零 Java 代码。
   **仅支持 Logback**（业务方切 log4j2 时不可 include，否则类缺失导致 logback 启动失败），README 明确声明。
5. 配置元数据：除 configuration-processor 自动生成外，补
   `META-INF/additional-spring-configuration-metadata.json`（默认值/弃用等说明）。

文件落点：
- `.../log/LogErrorProperties.java`
- `.../log/LogErrorAutoConfiguration.java`
- `.../log/AppenderBinder.java`
- `.../resources/META-INF/spring/...AutoConfiguration.imports`
- `.../resources/META-INF/additional-spring-configuration-metadata.json`
- `.../resources/includes/error-appender.xml`

验证：
- `ApplicationContextRunner` 断言：开/关 `tianshu.log.enabled`、有/无 `MqProducer` Bean 时
  sink 装配符合预期；`ApplicationReadyEvent` 后 appender 拿到 sink；无 Appender 时上下文不失败。
- 一个最小 Spring Boot 测试应用引 starter、挂 include，启动后打一条 error，断言经 `MqProducer`
  触发一次 `error-log` 发送（mock StreamBridge）。

退出标准：自动装配在缺 MQ/ES 依赖时不报错（优雅降级为仅缓冲/仅不发送）；含完整配置元数据。

---

### Step 5 — 消费：`error-log` 消费 → 指纹聚合 → 落 ES → 阈值告警

**依赖边**：依赖 Step 4。与 Step 3 无代码冲突，但逻辑上后做，便于联调。

任务：包 `com.gantang.tianshu.log.consumer`。

1. `ErrorLogConsumer extends AbstractMqConsumer<ErrorEvent>`（**复用现成模板，不另写平行 Consumer**）：
   - 构造传入 `tools.jackson.databind.ObjectMapper` 与 `TracePropagator`；
     `getPayloadType()` 返回 `ErrorEvent.class`。
   - `handle(MqMessage<ErrorEvent> message)`：调用持久化 + 告警；异常上抛由 binder retry/DLQ 处理。
   - `isDuplicate(...)`：可基于 msgId 做前置短路（若维护本地去重缓存）；最终幂等以 ES docId/脚本为准。
   - 说明真实 binder 形态：JSON 反序列化后 payload 是 `Map`，由模板 `objectMapper.convertValue`
     二次转换——本类不做 `instanceof MqMessage` 假设。
2. `ErrorLogPersistence`：用 `ElasticsearchService.client()` 落两类（`@ConditionalOnClass(ElasticsearchClient)`
   + `@ConditionalOnBean(ElasticsearchService)`；缺失则跳过、不报错）：
   - 原始：index `error-log-<yyyy.MM.dd>`，docId = event.msgId（同 ID 覆盖写，天然幂等）。
   - 聚合：index `error-agg-<yyyy.MM>`，docId = `fingerprint#<windowKey>`，
     **用 painless script upsert** 保证计数幂等：脚本维护 `seenMsgIds`（或对当前 msgId 判存），
     仅当 msgId 未见过时 `count++`、更新 lastSeen、追加 traceId（trace 集合设上限）。
     —— at-least-once 重投 / 重启后重放都不会让 count 翻倍。
3. 消费 Bean 的条件化与自动配置顺序：
   - 在自动配置中 `@Bean` 仅当 `tianshu.log.consumer-enabled=true` 时注册；
     消费侧配置类 `@AutoConfiguration(after = {MqAutoConfiguration.class, ElasticsearchAutoConfiguration.class})`。
   - 业务方（消费侧）**必须**提供以下绑定配置（starter 不假设默认 destination/group），
     starter 提供文档与可选 `application` 片段，README 给出：
     ```yaml
     tianshu:
       log:
         consumer-enabled: true
         consumer-group: error-log-diagnosis
     spring:
       cloud:
         function:
           definition: errorLogConsumer
         stream:
           bindings:
             errorLogConsumer-in-0:
               destination: error-log
               group: ${tianshu.log.consumer-group}
               content-type: application/json
     ```
     （RocketMQ binder 要求显式 group；默认 destination 取 binding 名本身，不会自动等于 `error-log`。）
4. 简单告警 `ErrorAlertRule`：**一期只做固定阈值**——同一 fingerprint 在窗口内 upsert 后
   `count >= tianshu.log.alert-threshold`（默认 20）即告警一次（用独立 logger
   `com.gantang.tianshu.log.ALERT`，不挂采集 Appender，避免回路）；可选转发到
   `tianshu.log.alert-destination`（缺省空=不转发）。环比/激增检测移二期。
   内存态 `ErrorAggregator` 仅用于进程内即时视图，**不作为权威计数**（权威计数在 ES 聚合文档）。

文件落点：
- `.../consumer/ErrorLogConsumer.java`
- `.../consumer/ErrorLogPersistence.java`
- `.../consumer/ErrorAlertRule.java`
- `.../consumer/AggregatedError.java`（视图对象）

验证：
- 转换测试：构造 payload 为 `LinkedHashMap`（模拟真实 binder 反序列化结果）的消息，
  经 consumer 后被正确 convertValue 为 `ErrorEvent`；同时覆盖裸 payload。
- 聚合幂等测试：同 msgId 重复 upsert 多次，count 不翻倍；不同 msgId 同指纹，count 正确递增、trace 去重。
- 原始落库：同 msgId 覆盖写仅一条；ES Bean 缺失时 handle 不报错（跳过落库）。

退出标准：消费幂等（重投/重启不产生重复原始 doc、计数不翻倍）；ES 缺失静默降级；
消费 Bean 在 `consumer-enabled=false` 时不被创建。

---

### Step 6 — 联调、文档、打包与提交

**依赖边**：依赖 1–5。

任务：
1. 工程根 `mvn -B clean install`（带测试）全绿。
2. 端到端冒烟（本地，建议 RocketMQ + ES；缺中间件时验证「降级 + 缓冲重放」路径）：
   - 生产侧示例应用引 `tianshu-log`（不开 consumer），触发一次 servlet 5xx 与一次 reactive error，
     断言：Appender 产出事件 → MQ `error-log` 收到；自身日志不产生回路。
   - 消费侧应用（可同进程，开 `consumer-enabled` + 绑定配置）断言：聚合文档 count 正确、
     原始文档存在；重复投递同 msgId 计数不翻倍；达阈值触发一次 ALERT。
   - 降级路径：停 MQ/ES，业务 error 不被阻断、缓冲（仅 worker）增长，恢复后重放。
   - 注意 `tianshu.mq.trace-enabled` / `envelope-enabled` 两个开关对 traceId 与信封形态的影响，冒烟覆盖默认值。
3. 在工程 `README.md`（当前为空）或 `tianshu-log/README.md` 写接入说明：
   依赖坐标、生产/消费两侧必需配置（含 `errorLogConsumer-in-0` 的 destination/group/content-type）、
   logback include 用法（仅 Logback）、脱敏与缓冲说明、红线（只分析建议、不自动执行）。
4. 清理临时文件；提交（作者沿用工程既有 git 配置；如需 `Coder <coder@tianshu.local>` 以实际工程约定为准，
   **提交前先 `git log -1` 核对作者，不擅自改 git config**）。

验证命令（Windows，JDK25）：
```powershell
mvn -B clean install
```

退出标准：全量构建 + 测试通过；README 含可照做的接入步骤；工作区无临时物。

---

## 并行性 / 模型分层

- 严格关键路径：Step 1 → 2 → 3 → 4 → 5 → 6。
- 可并行：Step 2 的脱敏单测与（Step 3 骨架文件）可在不同文件并行；
  Step 5 的 `ErrorAggregator`（纯逻辑）可在 Step 4 完成后即并行起步，落库部分待 Step 4。
- 模型分配：Step 3（异步/缓冲/不阻塞）与 Step 4（延迟绑定/降级）用最强模型并重点评审；
  Step 1/6 用默认模型；Step 2/5 逻辑清晰，默认模型即可。

## 回滚策略（逐步）

- 各步独立提交；任一步失败 `git revert <sha>` 即可，不影响既有模块（一期全部为**新增模块 + 三处聚合/BOM 追加行**）。
- 运行期风险隔离：业务方移除 logback include 或设 `tianshu.log.enabled=false` 即完全关闭，无需回滚版本。

## 安全与合规检查（评审清单）

- [ ] Appender 不向业务线程抛异常、不阻塞、不在业务线程做文件 IO（高频压测验证）。
- [ ] 自采集回路防护（包名跳过 + ThreadLocal 重入 + 独立告警 logger）。
- [ ] MQ/ES 不可用：缓冲仅在 worker 线程、受单文件/总字节上限约束（+ dropped 计数 + 告警），磁盘不被打满。
- [ ] 脱敏覆盖 message/stack/mdc；密钥、卡号、手机号、身份证不落 MQ/ES。
- [ ] 消费幂等（原始 docId 覆盖写 + 聚合 painless 脚本按 msgId 判存计数）。
- [ ] 生产/消费解耦：`consumer-enabled` 默认关，只发不收不被迫启动消费者。
- [ ] 不自动改代码/重启/改配置（一期仅采集与告警，天然满足；写入 README 红线，约束二期）。
- [ ] BOM 为唯一版本源；新模块 mq/es 依赖 optional，缺依赖可降级。

## 为后续期预留的接口约定（本期只定不实现）

- `ErrorEvent` / `AggregatedError` 字段保持稳定，二期 LLM 诊断直接消费聚合结果。
- ES `error-log-*`（原始）与 `error-agg-*`（聚合）索引命名固定，供二期 trace 时间线/发布相关性检索。
- 指纹作为未来知识库主键（`diagnosis_knowledge.fingerprint`），保证「同类错误一条知识」。
- 四期 MCP 只读诊断端点将复用 `ElasticsearchService` 与 `MqProducer`，不在本期引入。
