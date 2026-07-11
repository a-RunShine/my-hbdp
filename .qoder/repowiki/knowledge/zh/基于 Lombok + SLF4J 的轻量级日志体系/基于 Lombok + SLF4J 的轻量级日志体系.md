---
kind: logging_system
name: 基于 Lombok + SLF4J 的轻量级日志体系
category: logging_system
scope:
    - '**'
source_files:
    - src/main/resources/application.yaml
    - src/main/java/com/hmdp/config/WebExceptionAdvice.java
    - src/main/java/com/hmdp/controller/UploadController.java
    - src/main/java/com/hmdp/service/impl/UserServiceImpl.java
---

## 系统概览
本项目采用 Spring Boot 内置的 SLF4J + Logback 作为日志框架，通过 Lombok 的 `@Slf4j` 注解在各组件中注入 logger 实例，未引入独立的日志门面或第三方日志实现。日志输出以控制台为主，无独立文件/滚动策略配置。

## 关键文件与包
- 应用配置：`src/main/resources/application.yaml` — 统一声明 `logging.level.com.hmdp=debug`
- 全局异常处理：`com.hmdp.config.WebExceptionAdvice` — 使用 `log.error(e.toString(), e)` 记录运行时异常堆栈
- 业务控制器/服务：`UploadController`、`UserServiceImpl` 等通过 `@Slf4j` 注入 logger，使用 `log.debug(...)` 输出调试信息

## 架构与约定
- **Logger 获取方式**：所有需要日志的类均通过 `@Slf4j` 注解由 Lombok 生成 `private final Logger log;`，避免手动声明字段。
- **日志级别策略**：在 `application.yaml` 中将 `com.hmdp` 包整体设为 `debug`；生产环境可通过覆盖该属性调整。
- **结构化程度**：当前仅使用字符串模板占位（如 `log.debug("发送验证码成功!验证码：{}", code)`），未定义统一的 MDC/TraceId/请求上下文字段，也未对日志格式进行自定义。
- **异常日志**：仅在 `WebExceptionAdvice` 中对 `RuntimeException` 做集中捕获并输出错误堆栈，其他层未建立统一的异常日志规范。

## 开发者应遵循的规则
1. 需要日志时优先使用 `@Slf4j` 注解，不要自行 `new Logger` 或使用 `System.out`。
2. 使用占位符传参而非字符串拼接，保证性能与可读性。
3. 业务调试信息使用 `debug`，不可恢复的错误使用 `error`，避免在热路径打印过多日志。
4. 如需追踪请求链路，应在拦截器中向 MDC 写入 TraceId 并在日志格式中引用（当前尚未实现）。
5. 敏感信息（验证码、密码等）不应直接输出到日志，当前 `UserServiceImpl` 中打印验证码存在安全风险，需修正。