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

默认保留支付成功后扣库存的兼容模式。完成迁移并启用库存预占后，普通下单和购物车结算会先预占可售库存，付款时只确认销量，取消或超时后释放。拼团暂不预占，支付时从剩余可售库存扣减。旧订单没有预占记录时沿用原支付路径；无法履约的真实收款仍进入补偿退款。独立退款对账流水尚待完善。AI 客服默认关闭，启用前需自行配置服务提供方。真实支付、并发及部署环境需要单独验证。

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

## 商品评价分页

详情页每次加载 10 条已审核评价，支持最新、评分从高到低及评分从低到高排序，并展示该商品全部已审核有效评分的平均分与星级分布。请求失败时会显示重试入口，而不是“暂无评价”。

公开接口为 `GET /api/review/product/{productId}/page`，参数包括 `currentPage`（默认 1）、`size`（默认 10、最大 50）和 `sort`（`latest`、`rating_desc`、`rating_asc`）。响应 `data` 包含 `records`、`total`、`current`、`size`、`pages` 和 `summary`；统计仅包括上架商品中已通过审核的 1 至 5 星评价。用户信息仅包含公开展示名和受限头像，不返回完整用户实体。

旧的评价数组接口暂保留兼容。新前端依赖分页接口，部署时应配套更新后端；本轮接口变化不需要数据库迁移。

## 订单超时与图片目录

普通订单默认在创建 30 分钟后进入待关闭范围；定时任务启动后延迟一分钟开始扫描，此后每分钟扫描一次，每次最多关闭 200 笔。只处理未付款且无支付记录的普通订单，拼团订单沿用拼团过期流程。扫描有延迟和批次上限，30 分钟不是精确到秒的支付截止时间。兼容模式下关闭订单不回补库存；启用预占时，只释放仍有效的预占，重复关闭或迟到支付不会重复恢复库存。关闭后收到已验签的成功支付通知，会沿用补偿退款流程。

可在本地配置中调整：

```properties
order.unpaid-timeout-minutes=30
order.unpaid-expiry.enabled=true
order.unpaid-expiry.delay-ms=60000
```

超时分钟数允许 `1` 至 `10080`。普通订单发起支付宝支付时也会检查该期限，达到期限后，即使后台尚未扫描关闭也拒绝生成支付页面；购物车批次中任一普通订单超时，会拒绝整批支付。设置 `order.unpaid-expiry.enabled=false` 只关闭后台清理任务，不关闭支付入口的期限检查。更新代码后下次启动会扫描符合条件的历史待付款普通订单。缺少创建时间的历史记录保留兼容行为，拼团仍使用其原有生命周期。

图片默认保存到后端进程工作目录下的 `files/`，通过 `/api/img/文件名` 访问。可以使用环境变量 `FILE_STORAGE_ROOT` 或 JVM 参数 `-Dapp.upload-dir=绝对路径` 指定固定目录；更换启动工作目录时，应保持图片目录一致。运行时图片不在仓库中，克隆代码不会自动获得商品图片。

## 可选：启用普通订单库存预占

此功能默认关闭，更新代码本身不会预占当前数据库库存。新建数据库的表结构脚本已包含 `inventory_reservation`；已有数据库需由维护人员审查并手动执行：

```text
springboot/src/main/resources/db/migration/V20261023_01__inventory_reservation.sql
```

项目未集成 Flyway，这个脚本不会自动执行。脚本只新增预占账本，不重置商品库存、不回填或修改历史订单。建议先在隔离开发数据库中验证，维护窗口内停止各后端实例，备份后执行迁移，再在所有实例的本地配置中统一设置并重启：

```properties
inventory.reservation.enabled=true
inventory.reservation.max-held-orders=10
inventory.reservation.max-held-units=50
```

- 普通下单、购物车结算：库存条件扣减与预占记录写入处于同一事务，库存不足时订单创建失败。
- 单个用户默认最多同时持有 10 条有效预占订单记录、合计 50 件商品，额度必须为正整数；购物车多商品按实际订单记录计数。只有账本状态 1 占额度，消费或释放后自动恢复名额。达到上限时应先付款或取消已有订单。
- 后端在开始读商品和写订单前锁定买家，创建事务使用 READ COMMITTED 隔离级别，额度查询读取最新已提交的预占以及本事务已写入的记录；同一用户同时提交多个结算请求也按顺序检查额度。超过额度会使整个结算事务失败，不返回部分成功结果。
- 付款：预占转为已消耗，只确认销量，不再次扣库存；无预占记录的历史订单按原流程处理。
- 主动取消、超时关闭、不能履约的收款：仍有效的预占最多释放一次。下架后的预占也能释放；已消耗的预占不会按未付款取消来补库存。
- 拼团继续使用现有支付扣库存逻辑，其可使用的也是扣除普通订单预占后的可售库存。
- `product.stock` 表示可售库存。预占账本不等于实物盘点或独立支付流水，出入库操作仍须按实物和业务流程核对。

账本状态：`1` 预占、`2` 已消耗、`3` 已释放。停用前先停止新订单进入，等待或通过正常取消、超时流程处理全部有效预占，确认 `SELECT COUNT(*) FROM inventory_reservation WHERE state = 1;` 为零，再在维护窗口统一关闭各实例开关。存在有效预占时关闭开关，启动检查会拒绝启动，避免遗失释放记录。不要让启用与关闭预占的实例混合处理同一数据库。回退到不支持预占的旧版本前，也必须先处理完所有有效预占，不能只回退代码。

库存预占相关回归测试不等于真实 MySQL 并发与事务回滚验证；正式启用前仍需进行数据库和支付沙箱联调。支付渠道侧截止时间、已生成支付页面的关闭、多账号滥用防护和独立退款对账仍是后续工作。

## 下单重试与重复提交保护

普通下单 `POST /api/order`、购物车结算 `POST /api/order/cart/create` 支持可选请求头 `Idempotency-Key`，值为 16 至 128 位英文字母、数字、下划线或短横线。新前端会自动携带随机请求标识；未传请求头的旧客户端继续按原流程创建订单，不具备这项重试保护。

- 同一买家、同一种下单入口、同一个标识重复提交相同内容，会返回原订单或原结算批次，不重复创建或预占库存。普通下单指纹包括商品、数量、收货信息及备注；购物车指纹包括去重排序后的购物车 ID 和收货信息。后端仍以数据库中的商品价格和购物车数量为准。
- 同一标识用于不同内容返回 HTTP 409；非法标识返回 HTTP 400。标识不能替代登录鉴权。普通下单与购物车的标识空间互相独立。
- 购物车响应新增 `replayed` 和 `paymentAllowed`。原批次已付款、关闭或进入其他处理状态时，前端转到“我的订单”，不再请求支付；支付入口仍会独立核验当前订单状态。
- 网络失败保留重试标识。购物车创建成功后支付失败、刷新后再次结算相同内容，会沿用原批次。修改数量、地址或选中商品会生成新的前端标识。普通下单成功进入订单页后清除本次标识。
- 浏览器在当前标签页的 `sessionStorage` 中按买家和入口保存最多 20 个待处理请求，只保存内容指纹和随机标识，不保存收货信息明文。存储不可用时退化为页面内存；关闭标签页、清空存储、记录被淘汰或无法持久保存指纹时，不能保证跨页面重试复用，应先查看已有订单。

此功能无需数据库迁移，复用现有 `order.out_trade_no` 字段及索引；新请求对应 64 位十六进制商户订单号。后端在 READ COMMITTED 事务中先锁买家行，再查询既有订单、创建订单和预占库存。同一数据库的下单实例应统一更新，订单号与交易历史应保留。尚未进行真实 MySQL 并发测试，单元测试和浏览器模拟验证不能替代数据库锁竞争及支付沙箱联调。

## 我的订单统计与加载状态

订单页通过 `GET /api/order/my-summary` 获取当前登录用户的完整订单计数：`total` 为全部订单，`unpaid` 为状态 0，`pending` 为状态 0 和 1。使用单次数据库聚合查询，不受列表分页影响，也不再拉取前 100 条订单估算。接口不接受其他用户 ID；管理员和商家在此接口看到的也是自己的购买订单，而非全站或店铺销量。

统计失败显示“—”及独立重试入口，订单列表仍可正常使用；列表失败显示重新加载按钮，不再当作“没有订单”。列表与统计均忽略晚到的旧请求结果，避免快速翻页后显示错误页面。手机页面也保留统计及重试入口。本功能无需数据库迁移，发布时同步更新前后端。统计和列表分别请求，交易状态变化期间可能短暂存在读取时差。

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
