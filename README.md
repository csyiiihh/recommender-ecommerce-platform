# 在线销售平台

基于 Vue 3、Element Plus、Spring Boot、MyBatis Plus、MySQL 和 Redis 的图书销售平台，包含商品浏览、收藏、购物车、订单、拼团、评价及商家管理。

## 目录

- `vue/`：前端源码与依赖锁文件。
- `springboot/`：后端源码、测试与 Maven 配置。
- `online_sales_only_struct.sql`：数据库表结构，不包含业务数据。
- `springboot/交易补偿与库存说明.md`：交易处理与库存边界说明。

## 本地运行

1. 准备 JDK 17 或 21、Maven、Node.js、MySQL 和 Redis，安装前端依赖。
2. 创建 `online_sales` 数据库并导入表结构；需要演示商品时自行准备测试数据及图片。
3. 参考 `springboot/config/application-local.example.properties` 配置同目录的 `application-local.properties`。数据库密码、JWT 密钥、邮件授权码和支付配置应使用自己的值。
4. 在 IDE 中运行 `org.example.springboot.SpringbootApplication`，或在 `springboot` 目录执行 `mvn spring-boot:run`；默认后端端口为 `1234`。
5. 在 `vue` 目录执行 `npm ci`、`npm run serve`，访问终端显示的地址。前端 `/api` 与 `/alipay` 代理到 `http://localhost:1234`。

## 检查与构建

- 后端：在 `springboot` 目录执行 `mvn verify`。
- 前端：在 `vue` 目录执行 `npm run lint`、`npm run build`。

## 仓库内容

保留源码、测试、构建配置、依赖锁文件、必要静态资源、配置示例和表结构。依赖目录、构建产物、IDE 工程文件、阶段性工作笔记、真实本地配置、含业务数据的数据库备份及运行时上传文件均不纳入版本控制。

历史提交中的敏感配置已在迁移发布版本中脱敏；旧凭据仍应作废或轮换。支付功能需要自行配置与验证，本仓库不包含可直接使用的支付密钥。
