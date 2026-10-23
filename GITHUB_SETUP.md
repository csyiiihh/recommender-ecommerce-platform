# GitHub 仓库说明

本项目包含 Vue 前端与 Spring Boot 后端。前端使用 npm run serve，默认接口代理为 http://localhost:1234；后端启动类为 org.example.springboot.SpringbootApplication。

本地配置使用 springboot/config/application-local.properties，参考同目录 example 文件，并按运行配置加载 local 配置或设置对应环境变量。不要上传真实密码、支付密钥和邮箱授权码。

数据库表结构保留在 online_sales_only_struct.sql；含业务数据的数据库备份与 springboot/files 下的运行时上传文件不纳入新仓库，现有本地文件保留。

迁移说明：原始提交曾包含敏感配置，新账号发布分支对初始提交中的配置和上传文件进行了清理，因此提交哈希会改变。原始历史保留在本地分支和离线备份中。旧凭据须轮换，原仓库不受此次迁移自动修改。
