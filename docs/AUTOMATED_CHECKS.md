# 自动化检查与模拟回归

## 本地执行

后端需要 Java 17+ 与 Maven 3.9.x；测试使用 JUnit / Mockito，不要求连接业务数据库。Surefire 显式加载 Mockito agent，避免依赖 JVM 动态附加行为。

```bash
# springboot 目录
mvn verify
```

前端需要 Node 22。首次安装依赖与 Chromium 后，先构建，再运行仓库脚本：

```bash
# vue 目录
npm ci
npm run lint -- --no-fix
npm run build
npx playwright install chromium
npm run test:e2e
```

已有 Chrome 时可用 `CHROME_BINARY` 指定可执行文件绝对路径，无需安装 Playwright Chromium。例如 PowerShell：

```powershell
$env:CHROME_BINARY='C:/Program Files/Google/Chrome/Application/chrome.exe'
npm run test:e2e
```

脚本读取 `vue/dist`，通过 Playwright 路由拦截在内存中返回静态文件和模拟 API；无需启动前端服务器或业务后端。修改前端代码后必须重新构建，否则验证的是旧产物。报告写入被 Git 忽略的 `vue/test-results`，任一脚本失败即返回非零退出码。

## 场景与证据边界

| 脚本 | 场景数 | 主要检查 |
| --- | ---: | --- |
| `smoke.cjs` | 9 | 多层动态菜单、刷新、无效菜单、缺失组件、商城与拼团图片路径 |
| `product-form.cjs` | 4 | 商家范围、编辑器、直接上传、双击保存失败及重试 |
| `article-form.cjs` | 3 | 保存失败保留内容、重试更新封面、删除失败与重试 |
| `group-form.cjs` | 3 | 管理员编辑保留商家、分页契约、业务失败、HTTP 500 与双击重试 |
| `public-safety.cjs` | 3 | 公告 HTML 净化、资讯后端排序、拼团网络失败后复用请求标识 |

2026-10-26 本地执行共 22 个场景通过，无页面运行时异常；前端静态检查和生产构建通过。后端全量 359 项测试通过，无失败、错误和跳过。模拟接口不能证明 MySQL 锁、事务回滚、真实文件存储和支付渠道行为正确。

## GitHub Actions

[检查工作流](../.github/workflows/checks.yml) 在推送 main、创建 PR 或手动触发时运行，使用 Java 17 / Node 22，分别执行后端测试打包和前端检查、构建、浏览器回归，保存测试报告。工作流只有仓库读取权限，不部署、不读取支付凭据、不启动数据库。

本轮只验证了本地命令和工作流配置，未在 GitHub 上实际执行。推送后在 Actions → Project checks 查看两个任务；远程失败时依据上传报告修复，不以本地成功替代远程结果。

真实环境准备与业务验收见 [独立环境联调执行手册](INTEGRATION.md)。
