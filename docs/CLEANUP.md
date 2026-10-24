# 资讯访问边界与冗余清理

## 功能修复

- 公开资讯列表只使用状态 1，详情仅返回上架内容。管理员通过独立的 `/article/manage/page` 查询全部或下架内容；非管理员禁止访问。
- 阅读时在事务中锁定上架记录，以局部 SQL 更新浏览量，避免整行回写覆盖标题、正文、封面或状态。编辑忽略客户端传来的浏览量，上下架只写 ID 和状态，新建计数从 0 开始。
- 浏览量兼容历史空值和负值，并在整数上限停止增加；计数写入失败抛出异常。真实 MySQL 锁竞争与回滚仍需独立环境验证。
- 字典类型新增正确区分空查询与已存在记录；编码去除首尾空白，空编码拒绝保存，唯一约束冲突返回重复提示。数据库已有唯一索引，本次没有迁移。

## 删除范围

前端删除空 Vuex store，以及未被导入或注册的 AvatarUploader、Hints、Upload、AlgorithmSidebar 组件；清除 ProductManager 中被禁用的裁剪弹窗、状态、处理方法和专属样式，保留实际商品编辑弹窗内边距。

移除 Quill 及三个表格插件、vue-count-to、vuex、@vue/cli-plugin-vuex 七项前端直接依赖，同步 package-lock.json。wangEditor 继续用于商品与资讯富文本，xlsx/file-saver、ECharts、mitt 均保留。

后端删除未找到业务调用的 CacheUtil、DateUtils、RedisUtil、PageKeyGenerator、CacheConfig、PropertiesUtil 六个类及旧 config.properties；移除只有废弃 CacheConfig 引用的 Caffeine 依赖及旧配置项。RedisConfig 与受管理员权限限制的 Redis 诊断接口仍保留，不表示业务缓存已经接入。

后台 views 通过菜单 pagePath 动态加载，未据静态 import 缺失删除。旧推荐 API 和定时交易补偿逻辑不在本次删除范围。

商品与资讯管理共同使用 `vue/src/utils/imageUrl.js` 处理图片路径，修复商品编辑预览中的 `api//img/...`，兼容文件名、/img 路径、已有 /api 前缀和 HTTP(S) 完整地址。

## 验证与部署

新增 30 项后端回归，全量 clean verify 共 269 项通过。覆盖公开状态过滤、匿名及角色访问、浏览量空值/溢出/失败、局部写入与字典新增冲突等分支；检查 Mapper 实际生成的 SQL，但未连接真实数据库。

前端完整 lint 与生产构建通过，最终构建 hash 为 `07028eef95900d77`；本轮浏览器模拟共 35 个场景通过，图片地址另检查了 8 种输入形式。商品、资讯和失败的结算等待检查在最终构建后重跑，其余不受影响的交易场景使用本轮首次构建的通过记录。

前后端需配套更新，后台页面使用新的管理查询入口；重新安装前端依赖、构建前端，后端清除旧 target 后重新打包并完整重启。源代码删除不会自动清理手动部署目录内的旧 class 文件。

图片文件、业务数据库、支付配置和真实订单均未清理或修改。上传文件保留此前的策略，不能作为冗余代码一并删除。
