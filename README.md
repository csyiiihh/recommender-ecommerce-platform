# 在线销售平台

面向图书销售的前后端分离项目，包含用户商城、商家管理和平台管理。前端使用 Vue 3 与 Element Plus，后端使用 Spring Boot、MyBatis Plus 和 Spring Security，数据存储使用 MySQL，缓存使用 Redis 与 Caffeine。

## 功能与当前边界

| 模块 | 已有能力 |
| --- | --- |
| 商城 | 商品分类、搜索、详情、轮播公告、推荐与阅读偏好 |
| 用户 | 邮箱验证码注册、登录、收货地址、收藏和购物车 |
| 交易 | 普通订单、支付宝支付通知校验、发货签收、退款与自动补偿 |
| 拼团 | 活动、开团参团、成功后履约、失败补偿退款 |
| 评价 | 完成购买后评价、后台审核、公开展示受限用户信息 |
| 后台 | 商品、订单、物流、库存出入库、用户和内容管理 |

普通订单目前在**支付成功后扣库存**，下单时不预占。支付后库存不足等无法履约的情况进入补偿退款；库存预占和独立退款对账流水尚待完善。AI 客服默认关闭，启用前需自行配置服务提供方。真实支付、并发及部署环境需要单独验证。

## 项目结构

```text
.
├─ vue/                                      # 前端源码、构建配置与 package-lock.json
├─ springboot/
│  ├─ config/application-local.example.properties
│  ├─ src/main/java/org/example/springboot/    # 控制器、服务、持久层及定时任务
│  ├─ src/main/resources/                     # 公共配置与 Mapper 资源
│  ├─ src/test/                               # 回归测试
│  ├─ pom.xml
│  └─ 交易补偿与库存说明.md
└─ online_sales_only_struct.sql               # 仅表结构，不含用户或商品数据
```

## 环境准备

- Java：项目编译目标为 JDK 17，本地也可使用 JDK 21。
- Maven：3.9.x；首次构建需要下载依赖。
- Node.js / npm：本轮使用 Node.js 22.21.1 验证，依赖版本以 `package-lock.json` 为准。
- MySQL：8.x，默认库名 `online_sales`、端口 `3306`。
- Redis：默认地址 `localhost:6379`、数据库 `0`，有密码时需额外配置。

```powershell
git clone https://github.com/csyiiihh/recommender-ecommerce-platform.git
cd recommender-ecommerce-platform
```

## 1. 初始化数据库

从项目根目录运行 `mysql -u root -p`，登录自己的 MySQL 后执行：

```sql
CREATE DATABASE IF NOT EXISTS online_sales CHARACTER SET utf8mb4;
USE online_sales;
SOURCE online_sales_only_struct.sql;
```

表结构脚本包含重建表语句，请仅导入新建的开发数据库，已有数据的数据库应先备份并审查脚本。仓库没有演示商品、用户或默认管理员密码；空数据库启动后页面无商品属于正常情况，需要自行初始化测试数据。注册流程使用邮箱验证码，首次管理员需在自己的开发环境中初始化，不能把 `spring.security.user` 配置当作业务管理员账号。

## 2. 配置后端

复制配置示例（不要覆盖已有的本地配置）：

```powershell
if (!(Test-Path springboot/config/application-local.properties)) {
    Copy-Item springboot/config/application-local.example.properties springboot/config/application-local.properties
}
```

编辑本地文件，使用自己的配置值，或设置示例引用的环境变量：

| 配置 / 环境变量 | 用途 |
| --- | --- |
| `spring.datasource.username` / `DB_USERNAME` | MySQL 用户，示例默认 `root` |
| `spring.datasource.password` / `DB_PASSWORD` | MySQL 密码 |
| `jwt.secret` / `JWT_SECRET` | 必填，至少 32 个字符的随机密钥 |
| `spring.mail.username` / `MAIL_USERNAME` | 发件邮箱 |
| `spring.mail.password` / `MAIL_PASSWORD` | 邮件服务授权码 |
| `spring.data.redis.password` / `REDIS_PASSWORD` | Redis 密码，无密码可留空 |
| `alipay.app-id` / `ALIPAY_APP_ID` | 支付宝应用 ID |
| `alipay.private-key` / `ALIPAY_PRIVATE_KEY` | 应用私钥 |
| `alipay.public-key` / `ALIPAY_PUBLIC_KEY` | 支付宝公钥 |
| `alipay.seller-id` / `ALIPAY_SELLER_ID` | 收款方 ID，生产支付必须配置 |

数据库连接地址、Redis 主机和邮件服务器等也可以在本地文件中覆盖。当前邮件公共配置需要按自己的邮箱服务核对端口、SSL 等参数。支付配置可以在只浏览商品时留空，实际支付与回调则需要完整配置。

`application.properties` 已通过 `spring.config.import` 自动尝试加载 `./config/application-local.properties` 和 `./springboot/config/application-local.properties`，分别兼容从后端目录或项目根目录启动，无需额外激活 `local` profile。

## 3. 启动后端与前端

后端终端：

```powershell
cd springboot
mvn spring-boot:run
```

也可在 IDE 中运行 `org.example.springboot.SpringbootApplication`。确认控制台出现 `Started SpringbootApplication`，并监听 **1234** 端口。

另开终端，从项目根目录启动前端：

```powershell
cd vue
npm ci
npm run serve
```

前端优先使用 `8080`，被占用时以终端实际输出的地址为准，例如 `http://localhost:8081/`。启动命令是 **`npm run serve`**，项目没有 `dev` 或 `sever` 脚本。

开发服务器将 `/api` 和 `/alipay` 代理到 `http://localhost:1234`。后端端口变更时，需同步修改 `vue/vue.config.js` 并重启前端开发服务器。

## 订单超时与图片目录

普通订单默认在创建 30 分钟后进入待关闭范围；定时任务启动后延迟一分钟开始扫描，此后每分钟扫描一次，每次最多关闭 200 笔。只处理未付款且无支付记录的普通订单，拼团订单沿用拼团过期流程。扫描有延迟和批次上限，30 分钟不是精确到秒的支付截止时间。关闭过程不回补库存，因为未付款时没有扣减或预占库存；关闭后收到已验签的成功支付通知，会沿用补偿退款流程。

可在本地配置中调整：

```properties
order.unpaid-timeout-minutes=30
order.unpaid-expiry.enabled=true
order.unpaid-expiry.delay-ms=60000
```

超时分钟数允许 `1` 至 `10080`。设置 `order.unpaid-expiry.enabled=false` 可关闭此任务。更新代码后下次启动会扫描符合条件的历史待付款普通订单。

图片默认保存到后端进程工作目录下的 `files/`，通过 `/api/img/文件名` 访问。可以使用环境变量 `FILE_STORAGE_ROOT` 或 JVM 参数 `-Dapp.upload-dir=绝对路径` 指定固定目录；更换启动工作目录时，应保持图片目录一致。运行时图片不在仓库中，克隆代码不会自动获得商品图片。

## 检查、打包与部署

```powershell
# 在 springboot 目录
mvn verify
java -jar target/springboot-0.0.1-SNAPSHOT.jar

# 在 vue 目录
npm run lint -- --no-fix
npm run build
```

前端产物位于 `vue/dist/`，后端 JAR 位于 `springboot/target/`。部署静态前端时需配置路由回退，并将 `/api`、`/alipay` 转发到后端；Vue 开发服务器的代理配置不会自动进入生产部署。支付异步通知地址还需为支付宝可访问的地址，仅使用本机 `localhost` 无法接收外部回调。

本项目回归测试主要验证权限、订单状态、支付验签及幂等、补偿退款、库存和公开信息边界；单元测试通过不能替代真实 MySQL 并发测试和支付沙箱联调。更详细的交易边界见 [交易补偿与库存说明](springboot/交易补偿与库存说明.md)。

## 常见问题

| 现象 | 排查方向 |
| --- | --- |
| 所有接口报 500，前端终端出现 `ECONNREFUSED` | 检查后端是否监听 1234，以及 IDE 是否误启动了其他项目 |
| 后端提示 `JWT_SECRET must contain at least 32 characters` | 检查密钥长度与本地配置文件是否被加载 |
| 数据库连接失败 | 检查 MySQL 服务、数据库名、用户密码与连接地址 |
| 页面没有商品 | 先检查接口是否成功，再检查数据库是否有已上架商品；表结构脚本不提供数据 |
| 有商品文字但图片失败 | 检查 `/api/img/` 请求、图片是否存在及后端工作目录 |
| 注册收不到验证码 | 检查发件邮箱、授权码、SMTP 端口和服务器日志 |
| 支付或退款失败 | 检查沙箱/生产配置是否配套、签名、收款方及异步回调地址 |

## 版本控制

仓库保留源码、测试、依赖锁文件、构建配置、必要静态资源、配置示例和表结构。`node_modules`、`target`、`dist`、IDE 工程文件、临时报告、真实本地配置、含业务数据的 SQL 备份和上传目录均被忽略。

不要提交密码、邮件授权码、支付密钥或真实用户数据。迁移发布历史中的敏感配置已脱敏，原来使用过的凭据仍应作废或轮换。非必要文件的清理采用普通删除提交，旧版本中的非敏感文件仍保留在 Git 历史中。
