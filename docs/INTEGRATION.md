# 独立环境联调执行手册

## 当前状态

隔离环境的 Compose、后端示例配置、前置检查和只读冒烟脚本已提供；本轮没有创建数据库、导入 SQL、启动业务后端或调用支付渠道。当前没有可重置测试库和支付宝沙箱，因此真实交易、MySQL 并发与退款回调验收仍待环境就绪。

## 1. 准备独立终端与配置

使用 Node 22、JDK 17+、Docker Compose。复制 `.env.integration.example` 为被忽略的 `.env.integration`，填写独立测试密码。将相同密码设为当前终端的 `INTEGRATION_MYSQL_PASSWORD`，另设至少 32 字符的 `INTEGRATION_JWT_SECRET`。不要把密码、支付密钥或 `.env.integration` 提交到 Git。

在没有 ALIPAY_*、SPRING_*、JWT_SECRET、DB_PASSWORD、FILE_STORAGE_ROOT 和 Java 注入参数等业务覆盖变量的新终端运行：

```powershell
node scripts/integration/preflight.cjs
```

缺少参数或 Docker 时退出码为 2，不会启动服务。不要通过填入业务凭据绕过检查。

## 2. 启动专用基础设施

```powershell
docker compose --env-file .env.integration -f compose.integration.yml config --quiet
docker compose --env-file .env.integration -f compose.integration.yml up -d
```

仅绑定本机：MySQL 13316、Redis 13379，数据库名固定 `online_sales_integration`。命名卷属于 `zhiyue-integration` 项目，使用已有数据前先核对归属。启动不会自动导入任何 SQL。

```powershell
docker compose --env-file .env.integration -f compose.integration.yml exec mysql mysql -u root -p online_sales_integration
```

在 MySQL 终端先执行 `SELECT DATABASE();`，确认是 `online_sales_integration`，再执行 `SOURCE /seed/schema.sql;`。脚本含重建表语句，只允许用于本专用测试库；不要向现用业务库导入。初始化买家、商家、管理员及测试商品时使用自有测试信息。邮件注册另需接入本机 SMTP 测试接收器，示例配置目前指向关闭的本机端口，不能发送真实邮件。

## 3. 启动隔离后端

```powershell
# 项目根目录，先完成后端构建
powershell -File scripts/integration/start-backend.ps1 -Start
```

脚本先检查环境，关闭默认的 application-local.properties 导入，再固定使用 integration 示例配置；监听 13334，上传文件使用 JVM 参数指向项目内 `integration-uploads`。普通订单超时清理和库存预占默认关闭；拼团扫描与自动退款重试也默认关闭。启动器以命令行参数固定这些默认值，避免被同名环境变量意外覆盖。仅在已核对独立测试库、样本与支付配置，并有意验证定时任务时，才使用 `-Start -EnableBusinessTasks` 开启拼团扫描和退款重试；普通订单超时清理、库存预占仍保持关闭。不要混入真实待退款数据。

支付网关固定为无服务的本机地址，不能支付。真实沙箱验收前必须另行审查网关、配套沙箱证书、收款方和回调可达性，再在独立配置中启用。不要将生产密钥带入本示例环境。

## 4. 执行只读检查

```powershell
$env:INTEGRATION_API_BASE='http://127.0.0.1:13334'
node scripts/integration/public-smoke.cjs
```

脚本只允许上述专用地址，只读取商品和资讯分页。无样本时返回 2，不能把空库响应当作业务通过。它不会下单、扣库存、清空数据库、退款或调用支付。

## 5. 真实业务验收与证据

按 [业务验收清单](../springboot/隔离联调与验收清单.md) 顺序完成：权限角色 → 普通购买 → 幂等与抢库存 → 超时/付款竞争 → 发货退款 → 拼团 → 预占 → 对账与恢复。每个场景记录样本 ID、前后订单/库存/账本状态、沙箱交易与退款号、预期和实际结果；日志分享前去除 token、收货信息和密钥。

库存预占先在测试库审查迁移，统一启用所有实例后验证；默认模式与预占模式分别验收。退款重试应使用渠道稳定请求号，核对渠道金额和本地状态。真实测试没有执行前，结果只能标为“待执行”。

退出时可运行 `docker compose --env-file .env.integration -f compose.integration.yml stop`，保留专用数据。重置或删除卷属于单独维护操作，不在这些脚本中自动执行。


## 定时任务边界（2026-10-26）

业务环境默认保持 `group-buy.tasks.enabled=true`、`refund.retry.enabled=true`；仅隔离示例配置与启动器默认关闭。活动到期后会在事务中复核最新结束时间，将未完成团队标记失败并记录退款意图；到期扫描不会调用支付渠道。自动退款仍由独立重试任务按稳定退款请求号处理。

单次扫描最多 100 个记录，单条处理失败会记录日志并允许后续记录继续。活动关闭与待退款意图的数据库原子性、并发回调以及实际渠道恢复，仍需独立 MySQL 和沙箱验收；本地模拟测试不能证明这些场景已通过。
