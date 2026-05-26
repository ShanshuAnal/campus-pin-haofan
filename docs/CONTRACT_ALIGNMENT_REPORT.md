# 全局契约对齐报告

## 1. 本次发现的不一致问题

### 1.1 拼单 API 路径不一致

任务看板中 T009 的阻塞说明提到后端任务要求使用 `GET/POST /api/orders`，但 `docs/04-API接口文档.md` 已定义拼单接口为 `/api/group-orders`。

问题影响：

- 后端会话无法确定 Controller 路径；
- 前端会话无法确定 API 封装路径；
- 测试会话无法稳定编写接口用例。

### 1.2 用户字段命名不一致

数据库使用 `user.account` 作为登录账号字段，API 中部分公共响应示例仍出现 `account` 字段。

问题影响：

- 前端类型可能使用 `account`；
- 后端 VO 可能使用 `account`；
- 认证接口已要求使用 `username`，公共用户摘要却未完全跟进。

### 1.3 发起拼单缺少约定取餐地点字段

T009 需要校验取餐地点，但 `group_order` 表缺少 `pickup_location` 字段，API 发起拼单请求也缺少 `pickupLocation`。

问题影响：

- 后端无法落库发起拼单时约定的取餐地点；
- 前端无法按契约提交取餐地点；
- 拼单大厅和详情页无法展示约定取餐地点。

### 1.4 `pickup_record.pickup_location` 语义需要明确

`pickup_record` 可保留 `pickup_location`，但需要和 `group_order.pickup_location` 区分语义。

问题影响：

- 容易混淆“约定取餐地点”和“实际取餐地点”；
- 取餐状态更新接口字段含义不清。

### 1.5 增强能力边界需要继续保持

`notification_record`、Redis、RocketMQ 均为增强能力，不能成为 MVP 主流程硬依赖。

问题影响：

- 后端实现不能因为通知、缓存或消息队列未完成而阻塞拼单主流程；
- API 和数据库设计需要继续表达增强能力边界。

## 2. 最终统一决策

### 2.1 API 路径规范

拼单相关 API 统一使用：

```text
/api/group-orders
```

不使用：

```text
/api/orders
```

`docs/04-API接口文档.md` 是前后端接口唯一契约。

### 2.2 用户账号字段规范

数据库字段：

```text
user.account
```

API 对外字段：

```text
username
```

映射关系：

```text
username -> user.account
```

接口不得返回 `password_hash`。

### 2.3 取餐地点字段规范

`group_order.pickup_location`：

- 表示发起拼单时约定的取餐地点；
- 是发起拼单必填字段；
- 对应 API 字段 `pickupLocation`；
- 用于拼单大厅、详情页和后端创建拼单接口。

`pickup_record.pickup_location`：

- 表示实际取餐地点或履约阶段地点信息；
- 可为空；
- 为空时默认沿用 `group_order.pickup_location` 展示。

### 2.4 增强能力边界

`notification_record` 是增强表，MVP 主流程不强依赖。

Redis 和 RocketMQ 是增强能力，MVP 主流程不强依赖。

`schema.sql` 是数据库结构唯一来源。

`docs/04-API接口文档.md` 是前后端接口唯一契约。

## 3. 修改文件

- `docs/03-数据库设计.md`
- `docs/04-API接口文档.md`
- `sql/schema.sql`
- `sql/data.sql`
- `docs/TASK_BOARD.md`
- `docs/CONTRACT_ALIGNMENT_REPORT.md`

## 4. 对后端会话的影响

后端会话实现 T009 时应按以下契约继续：

- Controller 路径使用 `/api/group-orders`。
- 发起拼单请求必须接收并校验 `pickupLocation`。
- 创建 `group_order` 时写入 `pickup_location`。
- 返回用户信息时使用 `username` 字段，不使用 `account` 字段。
- Entity 和 Mapper 需要与最新 `sql/schema.sql` 对齐。
- 若本地数据库已基于旧版 schema 创建，需要重建数据库或执行字段迁移。

T009 的阻塞原因已解除，可交回后端会话继续实现。

## 5. 对前端会话的影响

前端会话后续实现时应按以下契约继续：

- 拼单 API 封装统一调用 `/api/group-orders`。
- 用户类型字段使用 `username`，不要使用 `account`。
- 发起拼单表单需要包含 `pickupLocation`。
- 拼单大厅和详情页可展示 `pickupLocation`。
- 取餐接口中的 `pickupLocation` 表示实际取餐地点或履约阶段地点，不替代拼单约定地点。

## 6. 后续任务恢复顺序

建议恢复顺序：

1. 后端会话继续 T009，实现拼单大厅查询和发起拼单，路径使用 `/api/group-orders`。
2. 后端会话同步实体字段，确保 `GroupOrder` 包含 `pickupLocation`。
3. 后端会话根据最新 `schema.sql` 重建或迁移本地数据库。
4. 后端完成 T009 后，测试会话补充拼单列表和创建拼单接口用例。
5. 前端会话根据 API 文档实现拼单大厅和发起拼单页面。

## 7. 验证结论

本次对齐后：

- 拼单 API 路径统一为 `/api/group-orders`。
- API 用户字段统一为 `username`，数据库仍使用 `user.account`。
- `group_order` 已补充 `pickup_location`。
- `pickup_record` 可记录实际取餐地点。
- `notification_record`、Redis、RocketMQ 仍为增强能力。
- T009 已从 `BLOCKED` 调整为 `TODO`，可恢复后端实现。
