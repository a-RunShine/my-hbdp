---
kind: configuration_system
name: Spring Boot 应用配置与基础设施装配体系
category: configuration_system
scope:
    - '**'
source_files:
    - src/main/resources/application.yaml
    - src/main/java/com/hmdp/config/MvcConfig.java
    - src/main/java/com/hmdp/config/MybatisConfig.java
    - src/main/java/com/hmdp/config/RedissonConfig.java
    - src/main/java/com/hmdp/config/WebExceptionAdvice.java
---

## 1. 系统概览
本项目采用 Spring Boot 2.3 作为统一配置入口，通过 `application.yaml` 集中声明数据库、Redis、Jackson、日志等运行参数；横切能力（MVC 拦截器链、MyBatis-Plus 分页插件、Redisson 分布式锁客户端、全局异常处理）以 `@Configuration` + `@Bean` 方式在 `com.hmdp.config` 包内显式装配，未使用 Spring Cloud Config / Nacos 等外部配置中心。

## 2. 核心文件与职责
- `src/main/resources/application.yaml`：应用级 YAML 配置，定义 server.port、spring.datasource、spring.redis、spring.jackson、mybatis-plus.type-aliases-package、logging.level 等。
- `config/MvcConfig.java`：注册登录校验与刷新令牌两个 Web 拦截器，并维护白名单路径模式。
- `config/MybatisConfig.java`：注入 MyBatis-Plus 分页插件（MySQL）。
- `config/RedissonConfig.java`：创建 RedissonClient Bean，直连本地 Redis。
- `config/WebExceptionAdvice.java`：基于 `@RestControllerAdvice` 的运行时异常统一收敛为 `Result.fail(...)`。

## 3. 架构与约定
- **单一配置文件**：所有环境相关参数集中在 `application.yaml`，无多 profile 拆分，也未引入 `.env` 或环境变量覆盖机制。
- **显式 Bean 装配**：第三方组件（Redisson、MP 插件）通过 Java Config 类暴露 Bean，而非依赖自动装配默认值。
- **硬编码连接信息**：Redisson 地址与密码直接写死在 `RedissonConfig`，与 `application.yaml` 中的 Redis 配置存在重复且不一致风险。
- **拦截器顺序**：`RefreshTokenInterceptor` order=0 先于 `LoginInterceptor` order=1 执行，形成“先刷新后鉴权”的链路。
- **白名单策略**：公开接口通过 `excludePathPatterns` 列表声明，新增公开端点需同步更新此处。

## 4. 开发者应遵循的规则
1. 新增运行期参数优先写入 `application.yaml`，避免在代码中散落的字符串常量。
2. 需要扩展第三方组件时，在 `config` 包下新建独立 `@Configuration` 类，以 `@Bean` 暴露实例。
3. 新增无需登录的公开 API 必须在 `MvcConfig.excludePathPatterns` 中补充对应路径模式。
4. 若后续引入多环境，建议按 `application-{profile}.yaml` 拆分并通过 `spring.profiles.active` 切换，同时把 Redisson 连接信息迁移到 YAML 中以消除硬编码。
5. 全局异常处理仅捕获 `RuntimeException`，业务异常如需区分可在此处扩展具体类型处理器。