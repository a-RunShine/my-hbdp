---
kind: build_system
name: Maven + Spring Boot 单工程构建
category: build_system
scope:
    - '**'
source_files:
    - pom.xml
---

本项目采用最简化的 Maven 单工程构建方式，未引入多模块、Docker、CI/CD 或自定义构建脚本。

- **构建工具与版本**：基于 `spring-boot-starter-parent:2.3.12.RELEASE`，Java 8，使用 `spring-boot-maven-plugin` 打包可执行 JAR，并通过 `<excludes>` 排除 Lombok 注解处理器。
- **依赖管理**：所有第三方库（Spring Web、MyBatis-Plus 3.4.3、Redisson 3.13.6、Hutool 5.7.17、MySQL Connector/J 5.1.47）均在根 `pom.xml` 中集中声明，无 `<dependencyManagement>` 或 BOM 聚合。
- **测试**：仅包含一个 `src/test/java/com/hmdp/HmDianPingApplicationTests.java`，通过 `spring-boot-starter-test` 运行，未见单元测试框架配置或覆盖率插件。
- **资源与数据库初始化**：应用启动时通过 `resources/db/hmdp.sql` 初始化 MySQL 数据；Lua 解锁脚本位于 `resources/unlock.lua`，由 Redisson 在分布式锁释放时执行。
- **CI/流水线**：仓库不存在 `.github/workflows`、Jenkinsfile、GitLab CI 等文件，`.github/modernize/java-upgrade/hooks/scripts/recordToolUse.sh` 仅为 AI 扩展的钩子脚本，不参与项目构建。
- **发布产物**：`target/` 目录为空，未见任何预构建产物或 Docker 镜像推送流程。

开发者只需执行 `mvn package` 或 `mvn spring-boot:run` 即可编译并运行应用。