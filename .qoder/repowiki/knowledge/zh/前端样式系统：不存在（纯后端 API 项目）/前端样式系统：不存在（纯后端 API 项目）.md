---
kind: frontend_style
name: 前端样式系统：不存在（纯后端 API 项目）
category: frontend_style
scope:
    - '**'
---

本仓库是一个纯后端 Spring Boot 应用，不包含任何前端代码。经全仓检索未发现 CSS、SCSS、HTML、JSP、Thymeleaf、Vue、React 等前端资源或模板文件，也未在 application.yaml 中配置静态资源目录或视图解析器。项目通过 REST Controller 暴露 JSON API，由外部前端工程消费，因此 `frontend_style` 类别不适用于此仓库。