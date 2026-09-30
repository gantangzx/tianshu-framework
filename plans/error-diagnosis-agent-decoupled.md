# 实施 Blueprint · 错误日志解耦采集 + 独立智能体服务（开箱即用）

> 目标：业务系统**零代码侵入**地把 ERROR 异常送出进程；采集端与"智能体服务"两个组件**物理解耦、独立部署**；
> 同时提供"引一个依赖即生效"的开箱即用体验（MQ / HTTP / 仅日志 三种传输自动选择、可降级）。
>
> 创建日期：2026-09-30
> 模式：直接模式（有 git、无 `gh`），就地编辑 + 提交 `main`
> 取代/衔接：本文件取代 `plans/error-log-diagnosis-phase1.md` 的形态。
> 旧 plan 把"采集 + 消费落 ES"放进同一个 starter、强耦合 ES；本蓝图改为
> **采集 starter（业务侧）** 与 **智能体服务（tianshu-agent 仓内的独立部署模块）** 两侧只通过 JSON 契约耦合。
> 旧 plan 中已验证可复用的设计（Appender 不阻塞、自采集回路防护、缓冲字节上限、生产/消费开关、
> 继承 `AbstractMqConsumer`）全部保留并迁移。
>
> 修订记录：
> - v2（2026-09-30，对抗评审后）：① 明确服务端多节点下聚合计数必须为**单语句原子条件更新**，禁止"先查后写"；
>   ② HTTP 传输的 JDK `HttpClient` 为阻塞调用，强约束只能在 worker 线程执行；
>   ③ 非 MQ 路径 traceId 取 MDC，钉死 MDC key 约定；④ 钉死契约件坐标/版本归属与本地→私服发布路径，防跨仓库漂移。
> - v3（2026-09-30）：智能体服务落点由"`D:\workspace\tianshu-diagnosis-server` 完全独立工程"
>   改为 **tianshu-agent 仓内的新模块 `tianshu-diagnosis-server`**（独立打 jar、独立进程部署，进程解耦不变）。
>   依据：二期大脑 `tianshu-core` 在同仓 reactor 构建，SNAPSHOT 联调零成本，无需为内部消费方提前发私服；
>   仓内已有可部署应用 `tianshu-app` 先例。契约件仍由框架仓发布；将来如需独立 CI/团队，可再整体拆出。

---

## 0. 关键事实（已核实，作为设计依据）

**业务侧工程 `D:\workspace\tianshu-framework`（Spring Boot 4.2.0-M1，Java 25，Servlet+Reactive 双栈）**

- 双栈异常处理器，5xx 统一 `log.error(..., ex)` 带完整堆栈：
  - `tianshu-common/src/main/java/com/gantang/tianshu/common/exception/servlet/GlobalExceptionHandler.java`
  - `tianshu-common/.../exception/reactive/ReactiveGlobalExceptionHandler.java`
- 方法切面 `tianshu-web/src/main/java/com/gantang/tianshu/web/log/LoggerAspect.java`（异常分支落 ERROR，双栈通用）。
- MQ 组件 `spring-boot-tianshu/tianshu-mq`：
  - 发送 `MqProducer.send(destination, eventType, bizKey, payload)`，底层 Spring Cloud Stream `StreamBridge`，
    统一信封 `MqMessage`（msgId/bizKey/eventType/timestamp/source/traceId/payload），自动带 traceId。
  - 消费模板 `AbstractMqConsumer<T>`：子类实现 `getPayloadType()` + `handle(MqMessage<T>)`，含 `isDuplicate` 钩子、
    trace 恢复、异常上抛交 binder retry/DLQ。
  - 自动装配 `MqAutoConfiguration`：`@ConditionalOnClass(StreamBridge.class)` + `tianshu.mq.enabled`（默认开），不绑死 binder。
  - **注意：MQ 组件当前使用 Jackson 3（`tools.jackson.databind.ObjectMapper`）。**
- 版本唯一来源 `tianshu-dependencies-bom/pom.xml`；starter 聚合在 `spring-boot-tianshu/pom.xml`。
- 工程内**无现成脱敏工具**（全仓核实），需新建。
- `tianshu-mq` 模块当前在 git 中仍是未跟踪状态（`?? spring-boot-tianshu/tianshu-mq/`），提交时一并注意。

**智能体侧 `d:\workspace\tianshu-agent`（Spring Boot 4.1.1，纯 WebFlux，0.1.0-SNAPSHOT）**

- `tianshu-core` 真零 Spring：仅依赖 reactor-core / jackson2 / snakeyaml / aviator / slf4j-api；
  `ReactiveAgent` 位于 core；另有零 Spring 的 `OpenAiCompatLlmClient`（JDK HttpClient，任意 OpenAI 兼容端点）。
- `tianshu-spring` 是"应用型"starter（强带 webflux+Netty/security/redis/JPA），**不在业务侧引入**。

**由此得出的两条设计结论**

1. 两侧**不共享实现 jar、不跨进程引依赖**，只通过本蓝图定义的 `ErrorEvent` JSON 契约耦合。
   → Boot 4.2 / 4.1、Jackson 3 / Jackson 2、Servlet / WebFlux 的差异被进程边界彻底隔离。
2. "开箱即用"靠**传输 SPI + 条件化自动装配**实现：有 MQ 走 MQ，无 MQ 可走 HTTP，再退化为仅日志，全程无需写 Java 代码。

---

## 目标交付物（本蓝图 DoD）

1. **契约件** `tianshu-diagnose-api`：`ErrorEvent` + 传输常量，零业务依赖（仅 Jackson 注解可选），双方共享。
2. **采集 starter** `tianshu-diagnose-log`（位于框架仓库 `spring-boot-tianshu/`）：
   - Logback `ErrorLogAppender`：只拦 ERROR（可配含带异常 WARN）、结构化、异步、本地缓冲、发送前脱敏、不阻塞业务线程。
   - 传输 SPI `ErrorEventSink` + 三实现（MQ / HTTP / Logging），按 classpath 与配置自动选择、自动降级。
   - 业务方"引依赖 + （可选）logback include + 少量配置"即生效，**不改一行业务代码**。
3. **智能体服务模块** `tianshu-diagnosis-server`（**tianshu-agent 仓内新模块**，独立打 jar / 独立进程部署，非框架模块）：
   - 依赖 `tianshu-core`（零 Spring 手动组装 Agent 的能力留待二期）。
   - 一期提供两个接收入口：HTTP `POST /api/v1/error-events` 与 MQ `error-log` 消费（二选一或并存）。
   - 接收 → 按 `ErrorEvent.fingerprint` 去重聚合（同指纹一条 + 计数/首末时间/影响 trace）→ 原始与聚合落存储。
4. 两侧版本进各自 BOM，`mvn clean install` 全绿，关键单测覆盖：指纹 / 脱敏 / Appender 不阻塞与回路 / 传输选择 / 接收幂等。

**非目标（本期不做，仅在末尾钉接口）**：LLM 根因诊断、向量知识库 RAG、人工反馈闭环、MCP 主动取证、前端页面、自动改代码/重启。

---

## 架构与数据流

```
┌──────────── 业务系统（引 tianshu-diagnose-log）────────────┐
│ GlobalExceptionHandler / ReactiveGlobalExceptionHandler /  │
│ LoggerAspect / 业务代码                                     │
│        │ log.error(msg, ex)                                 │
│        ▼                                                    │
│ Logback <root> ──► ErrorLogAppender                         │
│        │ ①级别过滤 ②组装 ErrorEvent+指纹 ③脱敏              │
│        │ ④有界队列(offer 即返回，满则 dropped++)            │
│        ▼                                                    │
│   单 worker 线程 ──► ErrorEventSink（传输 SPI）             │
│        ├─ MqErrorEventSink      （classpath 有 StreamBridge）│
│        ├─ HttpErrorEventSink    （配了 tianshu.diagnose.url）│
│        └─ LoggingErrorEventSink （兜底，结构化 JSON 日志）   │
│        下游不可用 → 本地缓冲文件(仅 worker, 字节上限)→恢复重放│
└───────────────────────────┬────────────────────────────────┘
                            │ MQ topic "error-log"  或  HTTP POST（二选一）
                            ▼
┌─ 智能体服务 tianshu-diagnosis-server（tianshu-agent 仓模块/独立进程）┐
│ 入口① MQ: ErrorEventConsumer extends AbstractMqConsumer     │
│ 入口② HTTP: POST /api/v1/error-events（同一收口方法）        │
│        ▼                                                    │
│ IngestService.receive(ErrorEvent)                           │
│   ① 校验 + 二次脱敏(纵深) ②指纹聚合(同指纹一条,count/时间/trace)│
│   ③ 原始事件存储(docId=msgId 幂等覆盖) ④聚合存储(幂等 upsert) │
│        ▼                                                    │
│ 存储：一期关系库(MySQL/PG) ；ES 作为可选检索(optional,可缺)  │
└─────────────────────────────────────────────────────────────┘
```

**红线（贯穿全蓝图）**

- `ErrorLogAppender.append()` 只做内存操作；任何下游故障只能"缓冲 + dropped 计数 + 告警"，**绝不抛回/拖慢业务线程**。
- 脱敏在采集端对 message/stack/mdc 统一处理；服务端入口做二次脱敏（纵深防御）。
- 智能体服务定位"接收 + 聚合（二期：分析 + 建议）"，**生产环境改代码/重启/变更配置必须人工审批，不自动执行**。
- 两侧只认 `ErrorEvent` JSON；契约版本化（字段 `schemaVersion`），新增字段只能向后兼容。

---

## 步骤拆分（一步一提交，按序执行；每步可冷启动）

### Step 1 — 契约：新建 `tianshu-diagnose-api`（零业务依赖）

**依赖边**：无（最先做，两侧都依赖它）。

**坐标归属（钉死，防跨仓库漂移）**：契约件随框架仓库一起发布，坐标固定
`com.gantang:tianshu-diagnose-api`（groupId 与框架一致，便于纳入框架 BOM 统一版本），版本跟框架 BOM（当前 `1.0.0-SNAPSHOT`）。
- 服务工程 `tianshu-diagnosis-server` 通过该坐标引用；开发期 `mvn install` 进本地仓库即可被解析，**不复制一份契约源码到服务工程**。
- 后续多人/CI 协作时发布到私服（Release 仓）；契约升级必须走 `schemaVersion`，不在两个仓库各改字段。

任务：
1. 新建 `spring-boot-tianshu/tianshu-diagnose-api/`（artifactId `tianshu-diagnose-api`），`pom.xml` 极简：
   - 仅可选依赖 Jackson 注解（或完全零依赖，用普通 POJO + Lombok）；不依赖 Spring / Logback / MQ。
2. 定义 `com.gantang.tianshu.diagnose.api.ErrorEvent`，字段：
   - `int schemaVersion`（当前 `1`）
   - `String msgId`（UUID 去横线，幂等键）
   - `String traceId`、`String appName`、`String env`、`String host`
   - `String level`、`String logger`
   - `String errorCode`（仅业务异常显式暴露 resultCode 时取，否则空）
   - `String exceptionClass`、`String message`
   - `java.util.List<String> stackFrames`（默认前 20 帧）
   - `String fingerprint`
   - `long timestamp`（epoch millis）
3. 定义常量类 `DiagnoseConstants`：默认 topic `error-log`、默认 HTTP path `/api/v1/error-events`、事件类型 `ErrorEvent`、`SCHEMA_VERSION=1`。

文件落点：
- 新增 `spring-boot-tianshu/tianshu-diagnose-api/pom.xml`
- `.../diagnose/api/ErrorEvent.java`、`.../diagnose/api/DiagnoseConstants.java`
- 改 `spring-boot-tianshu/pom.xml`（加 module）、`tianshu-dependencies-bom/pom.xml`（加坐标）

验证：
```powershell
mvn -B -pl spring-boot-tianshu/tianshu-diagnose-api -am clean install
```

退出标准：契约件可独立 install；零 Spring/Logback import；Jackson2 与 Jackson3 均能反序列化该 POJO（字段名稳定、无绑定专有类型）。

---

### Step 2 — 采集端骨架：新建 `tianshu-diagnose-log` 模块

**依赖边**：依赖 Step 1。

任务：
1. 新建 `spring-boot-tianshu/tianshu-diagnose-log/pom.xml`（parent `com.gantang:spring-boot-tianshu:1.0.0-SNAPSHOT`）。
   - 必选：`tianshu-diagnose-api`、`tianshu-common`、`org.slf4j:slf4j-api`。
   - optional / provided：
     - `tianshu-mq`（**optional**：MQ 传输按 `@ConditionalOnClass(StreamBridge)` 装配，缺失不报错）。
     - `ch.qos.logback:logback-classic`（provided，运行时业务方自带）。
     - `spring-context`、`spring-boot-autoconfigure`、`spring-cloud-stream`（provided）。
   - Jackson：跟随业务侧 Boot 4.2 的 **Jackson 3（`tools.jackson.*`）**，与 `tianshu-mq` 同栈；HTTP 传输用 JDK 原生 `java.net.http.HttpClient`，不引新 HTTP 库。
   - `spring-boot-configuration-processor`(optional)。
   - 测试依赖（工程当前零测试基建，本模块补齐）：`spring-boot-starter-test`(test)、`logback-classic`(test)。
2. `spring-boot-tianshu/pom.xml` 加 `<module>tianshu-diagnose-log</module>`。
3. BOM 在 starter 区加 `tianshu-diagnose-log` 坐标，version `${project.version}`。

退出标准：模块可独立 install；当前为空壳也不破坏其它模块；BOM/聚合均识别。

---

### Step 3 — 纯逻辑：指纹算法 + 脱敏（零 Spring，单测）

**依赖边**：依赖 Step 2。与 Step 4 文件不重叠，完成后可并行评审。

任务，包 `com.gantang.tianshu.diagnose.log`：
1. `ErrorFingerprinter`：
   - `fingerprint = md5_hex(exceptionClass + "|" + firstBusinessFrame + "|" + errorCode)`。
   - `firstBusinessFrame` = 堆栈中第一个业务包帧（前缀可配，默认 `com.gantang`），排除
     `java.*/jdk.*/sun.*/reactor.*/org.springframework.*/org.apache.*`；无业务帧退化为首个非 JDK 帧。
   - 无异常纯文本：`md5_hex(logger + "|" + normalize(message))`，`normalize` 去除数字/UUID/空白差异。
2. `ErrorSecretMasker`（新建，工程内无现成件）：覆盖手机号、身份证、银行卡、邮箱、密钥前缀
   （`sk-`、`ark-`、`password=`、`token=`）、SQL 字符串字面量；提供 `mask(String)`，保证正则本身不被畸形输入打挂（try/catch 兜底返回原文）。

单测：
- 同异常不同对象 ID → 同指纹；不同业务帧/异常类 → 不同指纹；业务帧缺失退化正确；纯文本归一化正确。
- 手机号/身份证/邮箱/密钥被替换，原文不出现；异常输入不抛错。

退出标准：单测全绿；这两类不 import 任何 `org.springframework` / logback。

---

### Step 4 — 采集：`ErrorLogAppender`（结构化 + 异步 + 本地缓冲 + 回路防护，风险最高）

**依赖边**：依赖 Step 3。**分配最强模型、重点评审。**

任务，包 `com.gantang.tianshu.diagnose.log.appender`：
1. `ErrorLogAppender extends AppenderBase<ILoggingEvent>`，可配 setter：
   `includeWarnWithThrowable`(false)、`stackTopN`(20)、`businessPackages`(`com.gantang`)、
   `queueCapacity`(8192)、`bufferDir`(`./.tianshu-diagnose-buffer`)、`maxBufferFileBytes`(64MB)、`maxBufferTotalBytes`(512MB)。
   - `append()` 仅内存操作：过滤级别 → 取 MDC（traceId 等）→ 组装 `ErrorEvent` + 指纹 → 脱敏 → `queue.offer`；
     offer 失败仅 `dropped++`。**业务线程无文件 IO、无阻塞、无异常可抛。**
   - **traceId 提取（钉死 key）**：优先 MDC key `traceId`，兼容 micrometer 常见 `traceId`/`X-B3-TraceId`；
     MQ 路径还会由 `MqProducer` 在发送时补当前 trace，但 HTTP/Logging 路径只能依赖此处从 MDC 取得，取不到留空，不做任何阻塞查询。
   - **自采集回路双保险**：① logger 名以 `com.gantang.tianshu.diagnose` 开头一律跳过；
     ② worker 线程 `ThreadLocal<Boolean>` 重入标记；告警用独立 logger `com.gantang.tianshu.diagnose.ALERT`（不挂本 Appender）。
2. 传输 SPI `ErrorEventSink`（`void publish(ErrorEvent)`，实现内部吞异常）+ 缓冲装饰：
   - `BufferingErrorEventSink`：**仅 worker 线程**在下游失败/未就绪时把事件以 JSONL 追加本地缓冲，
     受单文件/总字节上限约束，超限 `dropped++`；下游恢复重放并清理；缓冲 IO 异常只 warn。
   - 具体 MQ/HTTP/Logging 三实现放 Step 5，本步只定义 SPI 与缓冲装饰。
3. 异步：daemon 单线程 worker（命名 `tianshu-diagnose-sender`）+ 有界队列，取事件 → sink.publish。
   **不引入 Disruptor/tianshu-exchange**（重依赖代价过大）。
4. 生命周期：`start()` 启 worker（此时 Spring 未就绪，sink=仅缓冲；host 一次性解析缓存，不做反向 DNS 阻塞）；
   `stop()` 尽量 flush 后关线程（关闭期发送失败不重试，避免拖住停机）。

单测：
- 编程式 `LoggerContext` 挂载，触发 `log.error("x", new NPE())`，fake sink 断言事件字段/指纹；
  高频 10 万条不抛错、队列满 dropped 增长；业务线程无 IO（线程/计时验证）。
- 自采集日志不再被投递；sink 持续失败时缓冲仅在 worker 增长、达上限 dropped、恢复后重放清理。

退出标准：`append()` 路径无任何可外抛异常、无文件 IO；下游全挂不影响主流程。

---

### Step 5 — 装配与开箱即用：三传输实现 + 条件选择 + 延迟绑定

**依赖边**：依赖 Step 4。**核心，最强模型评审。**

任务，包 `com.gantang.tianshu.diagnose.log`：
1. `DiagnoseProperties`（`@ConfigurationProperties("tianshu.diagnose")`）：
   - 总开关 `enabled`(true)；`appName`(默认取 `spring.application.name`)；`env`（active profiles 逗号拼接）。
   - 传输：`transport`（`auto`/`mq`/`http`/`logging`，默认 `auto`）；
     `mq.destination`(默认 `error-log`)；`http.endpoint`（空=不启用）、`http.connectTimeoutMs`(2000)、`http.requestTimeoutMs`(3000)、`http.header-*`（可带认证头）。
   - Appender 参数镜像 Step 4 各可配项；`includeWarnWithThrowable/stackTopN/businessPackages/queueCapacity/bufferDir/maxBuffer*`。
2. 三传输实现（均 `implements ErrorEventSink`，内部吞异常、返回 void / 内部记结果）：
   - `MqErrorEventSink`：持 `MqProducer`，`send(destination,"ErrorEvent",fingerprint,event)`。
   - `HttpErrorEventSink`：JDK `java.net.http.HttpClient`，POST JSON 到 `endpoint`，短超时；非 2xx / IO 失败仅触发上层缓冲。
     - **阻塞约束**：`HttpClient` 同步 `send(...)` 是阻塞调用，**只能在 worker 线程执行**（该 Sink 仅被 worker 调用，禁止注入业务侧直接调用）；
       如确需非阻塞可用 `sendAsync`，但结果回调不得触碰业务线程上下文。
   - `LoggingErrorEventSink`：用独立 logger 输出结构化 JSON（兜底，零中间件）。
3. `DiagnoseAutoConfiguration`（`@AutoConfiguration(after = MqAutoConfiguration.class)`）：
   - `@ConditionalOnProperty(prefix="tianshu.diagnose", name="enabled", matchIfMissing=true)`。
   - 选择规则（auto）：classpath 有 `StreamBridge` 且存在 `MqProducer` Bean → MQ；否则配了 `http.endpoint` → HTTP；否则 Logging。
     显式 `transport=mq|http|logging` 时强制指定，条件不满足（如要 mq 但无 MqProducer）则 fail 回 Logging 并 warn，**不让上下文启动失败**。
   - 各实现用嵌套 `@Configuration` + `@ConditionalOnClass/@ConditionalOnBean/@ConditionalOnProperty` 隔离，避免引用缺失类导致装配失败。
4. `AppenderBinder`：监听 `ApplicationReadyEvent`，在 Logback `LoggerContext` 中找到已配置的 `ErrorLogAppender`，
   注入选中 sink、回填 appName/env、触发缓冲 flush；**找不到 Appender 仅 debug（业务方可能未 include），不报错**；
   `ContextClosedEvent` 调 `appender.stop()`。解决"Appender 早于容器 / 配置双份"：默认值兜底，Spring 就绪后以 yml 覆盖。
5. 资源：
   - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 写 `DiagnoseAutoConfiguration`。
   - `includes/error-appender.xml`：声明 appender 并 attach root，业务方在 logback-spring.xml `<include>`，零 Java；**仅支持 Logback**（切 log4j2 不可 include）。
   - `META-INF/additional-spring-configuration-metadata.json` 补默认值/枚举说明。

业务方开箱即用形态：
```xml
<dependency>
  <groupId>com.gantang</groupId>
  <artifactId>tianshu-diagnose-log</artifactId>
</dependency>
```
```yaml
tianshu:
  diagnose:
    transport: auto          # 有 MQ 走 MQ；下面这行存在且无 MQ 时走 HTTP
    http:
      endpoint: http://diagnosis-server:8081/api/v1/error-events
```

单测（`ApplicationContextRunner`）：
- 有/无 `MqProducer`、配/不配 `http.endpoint`、显式 transport、enabled 开关下，选中 sink 符合规则；条件不满足回落 Logging 且上下文不失败。
- 最小 Boot 测试应用挂 include，打一条 error，断言经所选 sink 发出一次（MQ mock StreamBridge / HTTP mockServer）。

退出标准：缺 MQ/HTTP 依赖时优雅降级到仅日志/缓冲；含完整配置元数据；无 Appender 时上下文正常。

---

### Step 6 — 智能体服务模块脚手架 `tianshu-diagnosis-server`（tianshu-agent 仓内模块，独立进程部署）

**依赖边**：依赖 Step 1 的契约（jar 已 install）。代码位于 agent 仓，但与 Step 2–5 分属不同仓库/进程，可由不同执行者并行起步。

任务：
1. 在 `d:\workspace\tianshu-agent\tianshu-diagnosis-server` **新建模块**（parent 即仓根 `com.gantang.tianshu:tianshu-agent:0.1.0-SNAPSHOT`），
   并在仓根 `pom.xml` 的 `<modules>` 追加 `<module>tianshu-diagnosis-server</module>`。
   - Boot / Java / WebFlux 基线随 agent 仓（Boot 4.1.1、Java 25、纯 WebFlux + Netty），端口默认 **8081**；独立打可执行 jar、独立起进程。
   - 默认依赖（保持轻量）：`com.gantang:tianshu-diagnose-api:1.0.0-SNAPSHOT`、`spring-boot-starter-webflux`、
     `spring-boot-starter-validation`、Lombok(optional)、`spring-boot-starter-test`(test)。
   - **MQ 依赖放入独立 `mq` Maven profile**：`spring-cloud-dependencies:2025.1.0` BOM（管 `spring-cloud-stream:5.0.0`，
     适配 Framework 7；旧 train `2025.0.0` 的 stream 4.3.0 在 Framework 7 下初始化 NPE，不可用）
     + `com.alibaba.cloud:spring-cloud-starter-stream-rocketmq:2023.0.3.3` + 显式补 `org.springframework.retry:spring-retry`
     （Alibaba 旧 binder 仍用 spring-retry，而 stream 5.x 不再传递）。
     默认构建不引、无需 MQ 即可 `install`/启动；开启 MQ 入口时 `-Pmq` 并提供 binder。
   - **不复用框架的 `tianshu-mq` / `AbstractMqConsumer`**：那是 Boot 4.2 + Jackson3 的框架实现件，引入即破坏"只共享契约"。
     MQ 入口用 Spring Cloud Stream 原生函数式 `Consumer`（见下）。
   - 一期先**不引 `tianshu-core`**，只做接收（Step 7 补聚合/存储）；`tianshu-core` 同仓，二期直接 reactor 引用做 LLM 诊断。
2. 两个入口，收口到同一 `IngestService.receive(ErrorEvent)`（本步为脚手架：校验 + 结构化接收，聚合/落库留 Step 7）：
   - HTTP：`ErrorEventController` 暴露 `POST /api/v1/error-events`，`@Valid @RequestBody ErrorEvent`，返回 **202 Accepted**。
   - MQ（`-Pmq` 时）：`ErrorEventStreamConsumer` 提供 `java.util.function.Consumer<org.springframework.messaging.Message<ErrorEvent>>`，
     Bean 名 `errorEventConsumer`，内部同样调用 `IngestService.receive`；幂等/去重交 Step 7，失败上抛交 binder retry/DLQ。
3. 配置 `application.yml`：端口 8081、应用名；MQ 绑定（destination `error-log` / group `tianshu-diagnosis-server` /
   content-type `application/json`）与 `spring.cloud.function.definition=errorEventConsumer` 集中放在 `---` 分段的 `mq` Spring profile，
   默认不启用（MQ 入口默认关、显式开启，与采集端"只发不收"一致）。
4. 主类 `DiagnosisServerApplication`（`@SpringBootApplication`）。

文件落点（均在 agent 仓）：
- 新增 `tianshu-diagnosis-server/pom.xml`、改仓根 `pom.xml`（加 module）。
- `.../diagnosis/server/DiagnosisServerApplication.java`、`.../diagnosis/server/web/ErrorEventController.java`、
  `.../diagnosis/server/core/IngestService.java`、`.../diagnosis/server/mq/ErrorEventStreamConsumer.java`（-Pmq 编译）、
  `src/main/resources/application.yml`。

验证：
```powershell
# 默认（无 MQ）：可独立启动，HTTP 202
mvn -B -pl tianshu-diagnosis-server -am clean install
```

退出标准：默认构建可独立启动、HTTP `POST /api/v1/error-events` 返回 202；`-Pmq` 下 MQ 入口可消费；
仅依赖 `tianshu-diagnose-api` 契约，不引任何业务框架实现 jar。

---

### Step 7 — 服务端：接收归一 → 指纹去重聚合 → 幂等存储

**依赖边**：依赖 Step 6。

任务，包 `com.gantang.tianshu.diagnosis.server`：
1. `IngestService.receive(ErrorEvent)`：校验 schemaVersion → 二次脱敏（纵深）→ 去重聚合 → 存储。
2. 持久化默认关闭、放入独立 **`persist` Maven profile**（默认构建保持纯 HTTP 轻量、无需 DB 即可启动）；
   构建 `-Ppersist` 并激活同名 Spring profile `persist`。两张表：
   - `error_seen_msg`：以 `msgId` 主键 + 唯一约束的幂等明细（fingerprint、seenAt）。
   - `error_aggregate`：以 `fingerprint` 聚合，字段 occurrenceCount / firstSeenAt / lastSeenAt /
     sampleTraceId / lastMessage / appName / env。
     - **Boot 4.1 实测环境暂无 Flyway 自动配置件**（spring-boot 4.1.1 各 jar 内无 FlywayAutoConfiguration），
       一期不引 Flyway，建表交 Hibernate `ddl-auto=update`（独立诊断库，可接受）。
     - **幂等/并发安全（事务内）**：先 `existsById(msgId)` 覆盖同一持久化上下文/已提交的重投递；
       再插入明细并 `saveAndFlush`，捕获 `DataIntegrityViolationException` 覆盖跨事务并发抢占；
       判定为首次后，用 **悲观写锁（PESSIMISTIC_WRITE）** 取聚合行自增（或新建）。
       不采用"无锁先 SELECT 再 UPDATE"的写法；同 msgId 重投计数不翻倍。
     - 一期不建原始表、不维护 trace 集合（聚合 + 幂等明细已满足"按指纹聚合 + 准确计数"）；trace 集合留待二期。
3. 存储为接口 `ErrorEventStore`：默认（未启用持久化）装配 no-op 实现，保证接收链路始终成立、服务可独立启动；
   `persist` profile 提供 `JpaErrorEventStore`。ES 检索作为二期**可选**实现（缺 ES 不报错），避免重蹈强耦合。
4. 异常处理：参数类非法输入抛 IllegalArgumentException，HTTP 入口映射 4xx 且不重试；
   持久化异常对 MQ 入口上抛交 retry/DLQ、对 HTTP 入口映射 5xx 由采集端缓冲重试。

单测：
- 默认 profile：`IngestServiceTest`（4 个）验证收口、契约校验拒绝、二次脱敏。
- `persist` profile：`JpaErrorEventStoreTest`（H2 PostgreSQL 模式，3 个）验证——
  同 msgId 重投聚合 count 不翻倍；不同 msgId 同指纹 count 递增；不同指纹互不干扰。
  该集成测试默认构建通过 compiler/surefire 排除，仅 `-Ppersist` 纳入。

退出标准：接收幂等（重投/重启不重复、计数不翻倍）；默认无需 DB 可启动；两入口行为一致。

---

### Step 8 — 端到端联调、两侧文档、打包提交

**依赖边**：依赖 Step 1–7。

任务：
1. 框架工程根 `mvn -B clean install` 全绿；agent 仓默认 `mvn -B clean install` 全绿，
   且 `mvn -Pmq -B -pl tianshu-diagnosis-server -am clean install`（MQ profile）全绿。
2. 端到端冒烟（本地，分两条传输路径各跑一遍）：
   - **MQ 路径**：示例业务应用引 `tianshu-diagnose-log`（transport=auto，提供 RocketMQ binder），触发一次 servlet 5xx 与一次 reactive error
     → MQ `error-log` → 服务端（开 MQ 入口）→ 原始/聚合正确；自采集无回路。
   - **HTTP 路径**：不提供 MQ，配 `http.endpoint` → 服务端 HTTP 入口 → 同样落库。
   - **降级路径**：停掉服务端，业务 error 不被阻断、缓冲（仅 worker）增长，恢复后重放成功；无 MQ 且无 endpoint 时退化为仅日志。
   - 幂等：重复投递同 msgId，服务端 count 不翻倍。
   - 覆盖 `tianshu.mq.trace-enabled/envelope-enabled` 默认值对 traceId/信封形态影响。
3. 文档：
   - 采集端 `tianshu-diagnose-log/README.md`：坐标、三种传输配置、logback include（仅 Logback）、脱敏/缓冲说明、红线。
   - 服务端（agent 仓）`tianshu-diagnosis-server/README.md`：启动、两入口配置、`mq` profile、存储与可选 ES、二期扩展点。
4. 清理临时文件（`build-*.bat/build-*.log`、`settings-aliyun.xml` 等确认是否为临时物；勿误删需保留件）；
   提交前 `git log -1` 核对作者，**不擅自改 git config**；注意 `tianshu-mq` 当前未跟踪，提交范围按需确认。

退出标准：两侧全量构建 + 测试通过；README 可照做；工作区无残留临时物。

---

## 并行性 / 依赖图 / 模型分层

- 关键路径：Step 1 → 2 → 3 → 4 → 5 → 8（采集侧）；服务侧 Step 1 → 6 → 7 → 8。
- **可并行**：
  - Step 3（指纹/脱敏）与 Step 6（服务模块脚手架）在 Step 1 后即可并行（不同仓库、不同文件）。
  - Step 4/5（采集核心，框架仓）与 Step 6/7（服务端，agent 仓）由不同执行者并行推进，仅在 Step 8 汇合联调。
- 模型分配：
  - 最强模型：Step 4（异步/缓冲/不阻塞/回路）、Step 5（条件装配/延迟绑定/降级）。
  - 默认模型：Step 1/2/3/6/8；Step 7 逻辑清晰默认即可（注意幂等 upsert 的并发正确性，安排评审）。

## 回滚策略

- 每步独立提交（框架仓、agent 仓各自提交）；一期为**两侧新增模块 + 聚合与 BOM 追加行**，任一步失败在对应仓 `git revert <sha>` 不影响既有模块。
- 运行期一键关闭：移除 logback include 或设 `tianshu.diagnose.enabled=false`；服务端下线不影响业务主链路（仅缓冲堆积，受字节上限保护）。

## 对抗评审清单（提交前逐项核对）

- [ ] `append()` 不抛异常、不阻塞、不在业务线程做 IO（高频压测 + 线程验证）。
- [ ] 回路防护：包名跳过 + ThreadLocal 重入 + 独立告警 logger。
- [ ] 缓冲仅在 worker、受单文件/总字节上限约束 + dropped 计数，磁盘不被打满。
- [ ] 脱敏覆盖 message/stack/mdc，服务端二次脱敏；密钥/卡号/手机/身份证不出进程、不落库。
- [ ] 传输选择：MQ/HTTP/Logging 按 classpath+配置自动选；条件不满足回落 Logging，上下文永不启动失败。
- [ ] 两侧仅依赖 `tianshu-diagnose-api` 契约，不跨进程引实现 jar；契约 `schemaVersion` 向后兼容。
- [ ] 接收幂等：raw 以 msgId 唯一、agg 为**单语句原子条件更新**（多副本并发安全，禁止先查后写），重投/重启不翻倍；HTTP/MQ 两入口结果一致。
- [ ] ES/中间件全部 optional，缺失静默降级；生产/消费与发送/接收开关默认安全。
- [ ] 不自动改代码/重启/改配置（写入两侧 README 红线，约束二期）。

---

## 为后续期钉死的接口约定（本期只定不实现）

- `ErrorEvent` 字段与 `schemaVersion` 保持向后兼容；二期 LLM 直接消费聚合结果。
- `error_fingerprint_agg.fingerprint` 即未来知识库主键（`diagnosis_knowledge.fingerprint`），保证"同类错误一条知识"。
- 二期：服务端引入 `tianshu-core`，手动组装 `ReactiveAgent` + `OpenAiCompatLlmClient`（零 Spring 件），
  流程 = 先查知识库 → 未命中则 LLM + 工具取证（代码/日志检索）→ 输出结构化"现象/根因/方案/验证/置信度"，低置信度转人工。
- 三期：向量库 RAG + 人工采纳/否决反馈校正排序；四期：业务侧只读 MCP 诊断端点支持实时取证。
- 版本对齐：待 Spring Boot 4.2 发布 **GA** 后，再评估两侧统一升级；当前跨进程只认 JSON，无需为 M1 版本对齐。
