# API 接口文档

## 1. 文档目的

本文档定义校园拼好饭系统 MVP 阶段前后端接口契约，覆盖登录、拼单大厅、发起拼单、加入拼单、拼单详情、锁单分摊、付款标记、取餐状态、我的拼单和基础数据看板。

接口字段基于 `docs/03-数据库设计.md`。数据库字段使用 `snake_case`，接口 JSON 字段使用 `camelCase`，两者保持一一映射，例如 `group_order.deadline_time` 对应 `deadlineTime`，`order_participant.payment_status` 对应 `paymentStatus`。

## 2. 通用约定

### 2.1 基础路径

```text
/api
```

### 2.2 统一响应格式

所有接口都使用统一响应格式。成功示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

失败示例：

```json
{
  "code": 400,
  "message": "拼单已锁定，不能继续加入",
  "data": null
}
```

### 2.3 认证方式

认证方案升级为 `accessToken + refreshToken` 的轻量真实登录：

- 支持注册、登录、获取当前用户、刷新 token、登出；
- 注册和登录使用 `username`、`password`；
- API 字段 `username` 映射数据库 `user.account`；
- 密码由后端加密后写入 `user.password_hash`，接口中不得返回密码或密码哈希；
- 登录成功返回 `accessToken`、`refreshToken`、`expiresIn` 和用户信息；
- `accessToken` 用于访问业务接口，`refreshToken` 用于换取新的 token；
- token 不得明文存储，只允许保存 SHA-256 摘要；
- 登出时将 `accessToken` 的 SHA-256 摘要写入 Redis 黑名单，并将对应 `refreshToken` 作废；
- accessToken 黑名单必须使用 Redis 存储，key 固定为 `auth:blacklist:access:{tokenHash}`，TTL 为 accessToken 剩余有效期；
- refreshToken 仍使用 MySQL `user_refresh_token.token_hash` 保存 SHA-256 摘要；
- 不引入 OAuth2、短信验证码、完整 Spring Security 或复杂 RBAC。

除注册、登录、刷新 token 接口外，其余 MVP 接口都需要登录态。前端通过请求头传递 accessToken：

```text
Authorization: Bearer <accessToken>
```

当前用户身份以 accessToken 解析结果为准，前端不得提交 `creatorId`、`userId` 等字段来替代后端鉴权。

认证存储方案以本文档为准：Redis 是 accessToken 黑名单的必选存储，MySQL `user_refresh_token` 表是 refreshToken 摘要和作废状态的最终存储。

### 2.4 状态枚举

拼单状态 `group_order.status`：

```text
CREATED, LOCKED, ORDERED, DELIVERING, ARRIVED, PICKED_UP, FINISHED, CANCELLED
```

MVP 阶段不定义 `EXPIRED` 状态。拼单超过截止时间但不继续推进时，由发起人通过状态接口手动取消，目标状态使用 `CANCELLED`。自动过期可作为 RocketMQ 增强功能后置，后续若新增 `EXPIRED`，必须先更新本文档、领域规则、数据库说明、后端枚举和测试用例。

付款状态 `order_participant.payment_status`、`payment_record.payment_status`：

```text
UNPAID, PAID, CONFIRMED, REFUNDED
```

取餐状态 `pickup_record.pickup_status`：

```text
WAITING_ORDER, WAITING_DELIVERY, ARRIVED, PICKED_UP, DISTRIBUTED
```

通知状态 `notification_record.status`：

```text
UNREAD, READ
```

### 2.5 金额与时间

- 金额字段与数据库 `decimal(10,2)` 对齐，接口中用数字或两位小数字符串均可，后端必须使用 `BigDecimal` 处理。
- 时间字段与数据库 `datetime` 对齐，接口中使用 `yyyy-MM-dd HH:mm:ss`。
- 金额分摊结果以后端锁单时落库的 `group_order` 和 `order_participant` 字段为准。

### 2.6 通用错误码

| code | 含义 | 场景 |
| --- | --- | --- |
| 200 | 成功 | 请求处理成功 |
| 400 | 参数错误或业务规则不满足 | 金额非法、状态不允许、人数已满 |
| 401 | 未登录或登录失效 | 缺少 accessToken、accessToken 过期或 refreshToken 失效 |
| 403 | 权限不足 | 非发起人锁单、非本人标记付款 |
| 404 | 数据不存在 | 拼单、参与记录、用户不存在 |
| 409 | 数据冲突 | 重复加入、重复确认、状态已变化 |
| 500 | 系统异常 | 未预期错误 |

## 3. 公共响应对象

### 3.1 用户摘要 UserSummary

| 字段 | 类型 | 数据库字段 | 说明 |
| --- | --- | --- | --- |
| `id` | number | `user.id` | 用户 ID |
| `username` | string | `user.account` | 登录账号，API 对外统一命名为 `username` |
| `nickname` | string | `user.nickname` | 昵称 |
| `phone` | string | `user.phone` | 联系方式 |
| `status` | string | `user.status` | 用户状态 |

### 3.2 拼单摘要 GroupOrderSummary

| 字段 | 类型 | 数据库字段 | 说明 |
| --- | --- | --- | --- |
| `id` | number | `group_order.id` | 拼单 ID |
| `title` | string | `group_order.title` | 拼单标题 |
| `orderType` | string | `group_order.order_type` | 拼单类型 |
| `merchantName` | string | `group_order.merchant_name` | 商家或窗口名称 |
| `pickupLocation` | string | `group_order.pickup_location` | 拼单约定取餐地点 |
| `creator` | UserSummary | `group_order.creator_id` | 发起人 |
| `deadlineTime` | string | `group_order.deadline_time` | 加入截止时间 |
| `maxParticipants` | number | `group_order.max_participants` | 最大参与人数 |
| `participantCount` | number | `group_order.participant_count` | 当前参与人数 |
| `discountThresholdAmount` | decimal | `group_order.discount_threshold_amount` | 满减门槛 |
| `discountAmount` | decimal | `group_order.discount_amount` | 配置优惠金额 |
| `originalTotalAmount` | decimal | `group_order.original_total_amount` | 原始总金额 |
| `actualDiscountAmount` | decimal | `group_order.actual_discount_amount` | 实际优惠金额 |
| `payableTotalAmount` | decimal | `group_order.payable_total_amount` | 应付总金额 |
| `roundingAdjustmentAmount` | decimal | `group_order.rounding_adjustment_amount` | 尾差调整金额 |
| `status` | string | `group_order.status` | 拼单状态 |
| `pickupUser` | UserSummary/null | `group_order.pickup_user_id` | 当前取餐人 |
| `remark` | string | `group_order.remark` | 拼单备注 |
| `lockedTime` | string | `group_order.locked_time` | 锁单时间 |
| `finishTime` | string | `group_order.finish_time` | 完成时间 |
| `cancelTime` | string | `group_order.cancel_time` | 取消时间 |
| `createTime` | string | `group_order.create_time` | 创建时间 |
| `updateTime` | string | `group_order.update_time` | 更新时间 |

### 3.3 参与者 Participant

| 字段 | 类型 | 数据库字段 | 说明 |
| --- | --- | --- | --- |
| `id` | number | `order_participant.id` | 参与记录 ID |
| `groupOrderId` | number | `order_participant.group_order_id` | 拼单 ID |
| `user` | UserSummary | `order_participant.user_id` | 参与用户 |
| `originalAmount` | decimal | `order_participant.original_amount` | 原始金额 |
| `discountShareAmount` | decimal | `order_participant.discount_share_amount` | 分摊优惠 |
| `payableAmount` | decimal | `order_participant.payable_amount` | 应付金额 |
| `roundingAdjustmentAmount` | decimal | `order_participant.rounding_adjustment_amount` | 尾差调整 |
| `paymentStatus` | string | `order_participant.payment_status` | 付款状态 |
| `paidMarkTime` | string | `order_participant.paid_mark_time` | 标记付款时间 |
| `paidConfirmTime` | string | `order_participant.paid_confirm_time` | 确认付款时间 |
| `joinTime` | string | `order_participant.join_time` | 加入时间 |
| `remark` | string | `order_participant.remark` | 成员备注 |
| `mealItems` | MealItem[] | `meal_item` | 餐品明细 |

### 3.4 餐品 MealItem

| 字段 | 类型 | 数据库字段 | 说明 |
| --- | --- | --- | --- |
| `id` | number | `meal_item.id` | 餐品条目 ID |
| `groupOrderId` | number | `meal_item.group_order_id` | 拼单 ID |
| `participantId` | number | `meal_item.participant_id` | 参与记录 ID |
| `itemName` | string | `meal_item.item_name` | 餐品名称 |
| `quantity` | number | `meal_item.quantity` | 数量 |
| `unitPrice` | decimal | `meal_item.unit_price` | 单价 |
| `subtotalAmount` | decimal | `meal_item.subtotal_amount` | 小计金额 |
| `remark` | string | `meal_item.remark` | 口味或备注 |

### 3.5 取餐记录 PickupRecord

| 字段 | 类型 | 数据库字段 | 说明 |
| --- | --- | --- | --- |
| `id` | number | `pickup_record.id` | 取餐记录 ID |
| `groupOrderId` | number | `pickup_record.group_order_id` | 拼单 ID |
| `pickupUser` | UserSummary | `pickup_record.pickup_user_id` | 取餐人 |
| `pickupLocation` | string/null | `pickup_record.pickup_location` | 实际取餐地点或履约阶段地点信息 |
| `pickupStatus` | string | `pickup_record.pickup_status` | 取餐状态 |
| `estimatedArrivalTime` | string | `pickup_record.estimated_arrival_time` | 预计到达时间 |
| `actualArrivalTime` | string | `pickup_record.actual_arrival_time` | 实际到达时间 |
| `pickedUpTime` | string | `pickup_record.picked_up_time` | 取餐完成时间 |
| `distributedTime` | string | `pickup_record.distributed_time` | 分发完成时间 |
| `remark` | string | `pickup_record.remark` | 取餐备注 |

## 4. 认证与用户接口

### 4.0 用户注册

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/auth/register` |
| 关联流程 | 登录前置账号创建 |
| 权限说明 | 无需登录。注册成功后创建用户账号，`username` 对应数据库 `user.account`，`nickname` 对应 `user.nickname`。后端必须加密密码并写入 `user.password_hash`，不得存储明文密码。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| body | `username` | string | 是 | 用户名或账号，映射到 `user.account` |
| body | `password` | string | 是 | 登录密码，仅用于注册提交，不在响应中返回 |
| body | `nickname` | string | 是 | 昵称，映射到 `user.nickname` |

请求示例：

```json
{
  "username": "20260001",
  "password": "P@ssw0rd123",
  "nickname": "小何"
}
```

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | number | 是 | 新注册用户 ID，对应 `user.id` |
| `username` | string | 是 | 用户名，对应 `user.account` |
| `nickname` | string | 是 | 昵称，对应 `user.nickname` |
| `status` | string | 是 | 用户状态，默认 `ACTIVE` |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "username": "20260001",
    "nickname": "小何",
    "status": "ACTIVE"
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `用户名不能为空` | 未提交 `username` 或去空格后为空 |
| 400 | `密码不能为空` | 未提交 `password` 或为空 |
| 400 | `昵称不能为空` | 未提交 `nickname` 或去空格后为空 |
| 409 | `用户名已存在` | `username` 与已有 `user.account` 重复 |

说明：

- 注册成功默认不等同于已登录；前端可继续调用 `POST /api/auth/login` 获取 `accessToken` 和 `refreshToken`。
- 如后端后续决定注册成功自动登录，必须先更新本文档。

### 4.1 用户登录

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/auth/login` |
| 关联流程 | 登录 |
| 权限说明 | 无需登录。账号存在、密码匹配且用户状态为 `ACTIVE` 时允许登录。登录失败不自动创建用户。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| body | `username` | string | 是 | 用户名，对应 `user.account` |
| body | `password` | string | 是 | 登录密码，用于与 `user.password_hash` 校验 |

请求示例：

```json
{
  "username": "20260001",
  "password": "P@ssw0rd123"
}
```

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `accessToken` | string | 是 | 后端签发的访问 token，用于 `Authorization` 请求头 |
| `refreshToken` | string | 是 | 后端签发的刷新 token，用于 `POST /api/auth/refresh` |
| `expiresIn` | number | 是 | accessToken 有效期秒数 |
| `user.id` | number | 是 | 用户 ID |
| `user.username` | string | 是 | 用户名，对应 `user.account` |
| `user.nickname` | string | 是 | 昵称 |
| `user.phone` | string/null | 否 | 联系方式 |
| `user.status` | string | 是 | 用户状态 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "access-token-demo",
    "refreshToken": "refresh-token-demo",
    "expiresIn": 7200,
    "user": {
      "id": 1001,
      "username": "20260001",
      "nickname": "小何",
      "phone": null,
      "status": "ACTIVE"
    }
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `用户名不能为空` | 未提交 `username` 或去空格后为空 |
| 400 | `密码不能为空` | 未提交 `password` 或为空 |
| 401 | `用户名或密码错误` | 用户不存在或密码校验失败 |
| 403 | `用户已禁用` | `user.status = DISABLED` |

说明：

- 登录成功后端需要创建有效的 refreshToken 会话记录，只保存 refreshToken 的 SHA-256 摘要，不保存明文 token。
- `accessToken` 明文仅返回给前端；后端不得落库明文 accessToken。
- accessToken 只有在 logout 后才写入黑名单，黑名单存储在 Redis，key 为 `auth:blacklist:access:{tokenHash}`，`tokenHash` 为 accessToken 的 SHA-256 摘要。

### 4.2 获取当前用户

| 项 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/auth/me` |
| 关联流程 | 登录态恢复 |
| 权限说明 | 已登录用户。需要携带 `Authorization: Bearer <accessToken>`。 |

请求参数：无。

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 是 | 用户 ID |
| `username` | string | 是 | 用户名，对应 `user.account` |
| `nickname` | string | 是 | 昵称 |
| `phone` | string/null | 否 | 联系方式 |
| `status` | string | 是 | 用户状态 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "username": "20260001",
    "nickname": "小何",
    "phone": null,
    "status": "ACTIVE"
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 401 | `未登录` | 未提交 Authorization 请求头 |
| 401 | `登录已失效` | accessToken 缺失、无效、过期或已命中 Redis 黑名单 |
| 404 | `用户不存在` | accessToken 中用户 ID 找不到对应用户 |

### 4.3 刷新 token

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/auth/refresh` |
| 关联流程 | 登录态续期 |
| 权限说明 | 无需携带 accessToken。必须提交有效 `refreshToken`，后端校验其 SHA-256 摘要、状态、过期时间和用户状态。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| body | `refreshToken` | string | 是 | 刷新 token 明文，仅用于本次请求校验，不得明文落库 |

请求示例：

```json
{
  "refreshToken": "refresh-token-demo"
}
```

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `accessToken` | string | 是 | 新签发的访问 token |
| `refreshToken` | string | 是 | 新签发的刷新 token，建议轮换 |
| `expiresIn` | number | 是 | 新 accessToken 有效期秒数 |
| `user.id` | number | 是 | 用户 ID |
| `user.username` | string | 是 | 用户名，对应 `user.account` |
| `user.nickname` | string | 是 | 昵称 |
| `user.phone` | string/null | 否 | 联系方式 |
| `user.status` | string | 是 | 用户状态 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "new-access-token-demo",
    "refreshToken": "new-refresh-token-demo",
    "expiresIn": 7200,
    "user": {
      "id": 1001,
      "username": "20260001",
      "nickname": "小何",
      "phone": null,
      "status": "ACTIVE"
    }
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `refreshToken 不能为空` | 未提交 `refreshToken` |
| 401 | `refreshToken 已失效` | refreshToken 不存在、已过期、已作废或 SHA-256 摘要校验失败 |
| 403 | `用户已禁用` | token 对应用户状态不是 `ACTIVE` |
| 404 | `用户不存在` | token 对应用户不存在 |

说明：

- refresh 成功时建议轮换 refreshToken：旧 refreshToken 记录作废，新建有效记录。
- refreshToken 明文只在请求和响应中出现，数据库仅保存 SHA-256 摘要。

### 4.4 用户登出

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/auth/logout` |
| 关联流程 | 退出登录 |
| 权限说明 | 已登录用户。需要携带 `Authorization: Bearer <accessToken>`，并提交当前 `refreshToken`。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| header | `Authorization` | string | 是 | `Bearer <accessToken>` |
| body | `refreshToken` | string | 是 | 当前会话 refreshToken，用于定位并作废 token 会话 |

请求示例：

```json
{
  "refreshToken": "refresh-token-demo"
}
```

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `logout` | boolean | 是 | 是否已完成登出处理 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "logout": true
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `refreshToken 不能为空` | 未提交 `refreshToken` |
| 401 | `未登录` | 未提交 Authorization 请求头 |
| 401 | `登录已失效` | accessToken 缺失、无效或过期 |
| 401 | `refreshToken 已失效` | refreshToken 不存在、已过期或已作废 |

说明：

- logout 时后端必须将当前 accessToken 的 SHA-256 摘要加入 Redis 黑名单，避免未过期 accessToken 继续访问接口。
- Redis 黑名单 key 固定为 `auth:blacklist:access:{tokenHash}`，TTL 固定为 accessToken 剩余有效期。
- logout 时后端必须将对应 refreshToken 记录标记为作废。
- accessToken 和 refreshToken 均不得明文存储；refreshToken 摘要仍保存在 `user_refresh_token.token_hash`。

## 5. 拼单接口

### 5.1 拼单大厅列表

| 项 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/group-orders` |
| 关联流程 | 拼单大厅 |
| 权限说明 | 已登录用户可查看。默认优先返回 `CREATED`、`LOCKED`、`ORDERED` 等未结束拼单，`FINISHED` 和 `CANCELLED` 可通过状态筛选查看。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| query | `status` | string | 否 | 拼单状态 |
| query | `orderType` | string | 否 | `TAKEOUT`、`CANTEEN`、`MILK_TEA`、`MIDNIGHT_SNACK` |
| query | `keyword` | string | 否 | 匹配 `title` 或 `merchantName` |
| query | `pageNum` | number | 否 | 页码，默认 1 |
| query | `pageSize` | number | 否 | 每页条数，默认 10 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 2001,
        "title": "奶茶满 60 减 10",
        "orderType": "MILK_TEA",
        "merchantName": "一号门奶茶",
        "pickupLocation": "一教大厅门口",
        "creator": {
          "id": 1001,
          "username": "20260001",
          "nickname": "小何",
          "phone": null,
          "status": "ACTIVE"
        },
        "deadlineTime": "2026-05-27 18:30:00",
        "maxParticipants": 6,
        "participantCount": 3,
        "discountThresholdAmount": 60.00,
        "discountAmount": 10.00,
        "originalTotalAmount": 68.00,
        "actualDiscountAmount": 0.00,
        "payableTotalAmount": 68.00,
        "roundingAdjustmentAmount": 0.00,
        "status": "CREATED",
        "pickupUser": null,
        "remark": "下课后一起取",
        "lockedTime": null,
        "finishTime": null,
        "cancelTime": null,
        "createTime": "2026-05-27 18:00:00",
        "updateTime": "2026-05-27 18:10:00"
      }
    ]
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `status 不合法` | 状态枚举不存在 |
| 401 | `登录已失效` | 未登录 |

### 5.2 发起拼单

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/group-orders` |
| 关联流程 | 发起拼单 |
| 权限说明 | 已登录用户。`creatorId` 由 token 对应用户确定，不允许前端传入。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| body | `title` | string | 是 | 对应 `group_order.title` |
| body | `orderType` | string | 是 | 对应 `group_order.order_type` |
| body | `merchantName` | string | 是 | 对应 `group_order.merchant_name` |
| body | `pickupLocation` | string | 是 | 对应 `group_order.pickup_location`，拼单约定取餐地点 |
| body | `deadlineTime` | string | 是 | 对应 `group_order.deadline_time` |
| body | `maxParticipants` | number | 否 | 对应 `group_order.max_participants` |
| body | `discountThresholdAmount` | decimal | 否 | 对应 `group_order.discount_threshold_amount` |
| body | `discountAmount` | decimal | 否 | 对应 `group_order.discount_amount`，未提交按 `0.00` |
| body | `remark` | string | 否 | 对应 `group_order.remark` |

请求示例：

```json
{
  "title": "奶茶满 60 减 10",
  "orderType": "MILK_TEA",
  "merchantName": "一号门奶茶",
  "pickupLocation": "一教大厅门口",
  "deadlineTime": "2026-05-27 18:30:00",
  "maxParticipants": 6,
  "discountThresholdAmount": 60.00,
  "discountAmount": 10.00,
  "remark": "下课后一起取"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2001,
    "title": "奶茶满 60 减 10",
    "orderType": "MILK_TEA",
    "merchantName": "一号门奶茶",
    "pickupLocation": "一教大厅门口",
    "creator": {
      "id": 1001,
      "username": "20260001",
      "nickname": "小何",
      "phone": null,
      "status": "ACTIVE"
    },
    "deadlineTime": "2026-05-27 18:30:00",
    "maxParticipants": 6,
    "participantCount": 0,
    "discountThresholdAmount": 60.00,
    "discountAmount": 10.00,
    "originalTotalAmount": 0.00,
    "actualDiscountAmount": 0.00,
    "payableTotalAmount": 0.00,
    "roundingAdjustmentAmount": 0.00,
    "status": "CREATED",
    "pickupUser": null,
    "remark": "下课后一起取",
    "lockedTime": null,
    "finishTime": null,
    "cancelTime": null,
    "createTime": "2026-05-27 18:00:00",
    "updateTime": "2026-05-27 18:00:00"
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `title 不能为空` | 标题缺失 |
| 400 | `pickupLocation 不能为空` | 约定取餐地点缺失 |
| 400 | `deadlineTime 必须晚于当前时间` | 截止时间非法 |
| 400 | `discountAmount 不能小于 0` | 优惠金额非法 |
| 401 | `登录已失效` | 未登录 |

### 5.3 拼单详情

| 项 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/group-orders/{id}` |
| 关联流程 | 拼单详情、凑单计算、付款和取餐展示 |
| 权限说明 | 已登录用户可查看。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 拼单 ID，对应 `group_order.id` |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "order": {
      "id": 2001,
      "title": "奶茶满 60 减 10",
      "orderType": "MILK_TEA",
      "merchantName": "一号门奶茶",
      "pickupLocation": "一教大厅门口",
      "creator": {
        "id": 1001,
        "username": "20260001",
        "nickname": "小何",
        "phone": null,
        "status": "ACTIVE"
      },
      "deadlineTime": "2026-05-27 18:30:00",
      "maxParticipants": 6,
      "participantCount": 3,
      "discountThresholdAmount": 60.00,
      "discountAmount": 10.00,
      "originalTotalAmount": 68.00,
      "actualDiscountAmount": 10.00,
      "payableTotalAmount": 58.00,
      "roundingAdjustmentAmount": 0.01,
      "status": "LOCKED",
      "pickupUser": null,
      "remark": "下课后一起取",
      "lockedTime": "2026-05-27 18:31:00",
      "finishTime": null,
      "cancelTime": null,
      "createTime": "2026-05-27 18:00:00",
      "updateTime": "2026-05-27 18:31:00"
    },
    "participants": [
      {
        "id": 3001,
        "groupOrderId": 2001,
        "user": {
          "id": 1001,
          "username": "20260001",
          "nickname": "小何",
          "phone": null,
          "status": "ACTIVE"
        },
        "originalAmount": 22.00,
        "discountShareAmount": 3.24,
        "payableAmount": 18.76,
        "roundingAdjustmentAmount": 0.01,
        "paymentStatus": "UNPAID",
        "paidMarkTime": null,
        "paidConfirmTime": null,
        "joinTime": "2026-05-27 18:05:00",
        "remark": "少冰",
        "mealItems": [
          {
            "id": 4001,
            "groupOrderId": 2001,
            "participantId": 3001,
            "itemName": "珍珠奶茶",
            "quantity": 1,
            "unitPrice": 22.00,
            "subtotalAmount": 22.00,
            "remark": "少冰"
          }
        ]
      }
    ],
    "pickupRecord": null
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 404 | `拼单不存在` | `group_order.id` 不存在 |
| 401 | `登录已失效` | 未登录 |

### 5.4 加入拼单

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/group-orders/{id}/participants` |
| 关联流程 | 成员加入、凑单计算 |
| 权限说明 | 已登录用户。只能在 `group_order.status = CREATED` 且未过截止时间、人数未满时加入。同一用户不能重复加入同一拼单。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 拼单 ID |
| body | `remark` | string | 否 | 对应 `order_participant.remark` |
| body | `mealItems` | array | 是 | 至少一条餐品 |
| body.mealItems | `itemName` | string | 是 | 对应 `meal_item.item_name` |
| body.mealItems | `quantity` | number | 是 | 对应 `meal_item.quantity`，必须大于 0 |
| body.mealItems | `unitPrice` | decimal | 是 | 对应 `meal_item.unit_price`，必须大于 0 |
| body.mealItems | `remark` | string | 否 | 对应 `meal_item.remark` |

请求示例：

```json
{
  "remark": "少冰，不要吸管",
  "mealItems": [
    {
      "itemName": "珍珠奶茶",
      "quantity": 1,
      "unitPrice": 22.00,
      "remark": "少冰"
    }
  ]
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "participant": {
      "id": 3001,
      "groupOrderId": 2001,
      "user": {
        "id": 1002,
        "username": "20260002",
        "nickname": "小林",
        "phone": null,
        "status": "ACTIVE"
      },
      "originalAmount": 22.00,
      "discountShareAmount": 0.00,
      "payableAmount": 22.00,
      "roundingAdjustmentAmount": 0.00,
      "paymentStatus": "UNPAID",
      "paidMarkTime": null,
      "paidConfirmTime": null,
      "joinTime": "2026-05-27 18:08:00",
      "remark": "少冰，不要吸管",
      "mealItems": [
        {
          "id": 4001,
          "groupOrderId": 2001,
          "participantId": 3001,
          "itemName": "珍珠奶茶",
          "quantity": 1,
          "unitPrice": 22.00,
          "subtotalAmount": 22.00,
          "remark": "少冰"
        }
      ]
    },
    "orderAmount": {
      "participantCount": 1,
      "originalTotalAmount": 22.00,
      "discountThresholdAmount": 60.00,
      "discountAmount": 10.00,
      "actualDiscountAmount": 0.00,
      "payableTotalAmount": 22.00
    }
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `餐品不能为空` | 未提交餐品 |
| 400 | `餐品金额必须大于 0` | 数量或单价非法 |
| 400 | `拼单已过截止时间` | `deadline_time` 已过 |
| 400 | `拼单已锁定，不能继续加入` | 状态不是 `CREATED` |
| 409 | `不能重复加入同一拼单` | 违反 `group_order_id + user_id` 唯一规则 |
| 409 | `拼单人数已满` | 超过 `max_participants` |
| 404 | `拼单不存在` | 拼单 ID 无效 |

### 5.5 锁定拼单并生成分摊

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/group-orders/{id}/lock` |
| 关联流程 | 锁定拼单、优惠分摊 |
| 权限说明 | 仅拼单发起人可操作。只能从 `CREATED` 流转到 `LOCKED`。锁单时后端重新汇总 `meal_item.subtotal_amount` 和 `order_participant.original_amount`，生成订单级和成员级分摊字段。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 拼单 ID |
| body | `remark` | string | 否 | 锁单备注，可写入状态日志 |

请求示例：

```json
{
  "remark": "人数已够，准备下单"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "order": {
      "id": 2001,
      "status": "LOCKED",
      "originalTotalAmount": 68.00,
      "actualDiscountAmount": 10.00,
      "payableTotalAmount": 58.00,
      "roundingAdjustmentAmount": 0.01,
      "lockedTime": "2026-05-27 18:31:00"
    },
    "allocations": [
      {
        "participantId": 3001,
        "userId": 1001,
        "originalAmount": 22.00,
        "discountShareAmount": 3.24,
        "payableAmount": 18.76,
        "roundingAdjustmentAmount": 0.01,
        "paymentStatus": "UNPAID"
      },
      {
        "participantId": 3002,
        "userId": 1002,
        "originalAmount": 24.00,
        "discountShareAmount": 3.53,
        "payableAmount": 20.47,
        "roundingAdjustmentAmount": 0.00,
        "paymentStatus": "UNPAID"
      }
    ]
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `拼单没有参与者，不能锁单` | 无参与记录 |
| 400 | `发起人需先加入拼单以承接尾差` | 发起人未作为参与者加入 |
| 400 | `优惠金额不能大于原始总金额` | 金额规则不满足 |
| 403 | `只有发起人可以锁单` | 当前用户不是 `creator_id` |
| 409 | `当前状态不能锁单` | 状态不是 `CREATED` |

### 5.6 拼单主状态推进口径

MVP 阶段不再提供或使用 `PATCH /api/group-orders/{id}/status` 作为完成拼单接口，前端不得调用该路径完成拼单。

拼单主状态由已实现的业务接口同步推进：

| 触发动作 | 接口 | 主状态结果 |
| --- | --- | --- |
| 锁定拼单 | `POST /api/group-orders/{id}/lock` | `CREATED -> LOCKED` |
| 指定取餐人 | `PUT /api/group-orders/{id}/pickup-assignee` | `LOCKED -> ORDERED` |
| 更新取餐状态为 `WAITING_DELIVERY` | `POST /api/group-orders/{id}/pickup-status` | 同步为 `DELIVERING` |
| 更新取餐状态为 `ARRIVED` | `POST /api/group-orders/{id}/pickup-status` | 同步为 `ARRIVED` |
| 更新取餐状态为 `PICKED_UP` | `POST /api/group-orders/{id}/pickup-status` | 同步为 `PICKED_UP` |
| 更新取餐状态为 `DISTRIBUTED` | `POST /api/group-orders/{id}/pickup-status` | 同步为 `FINISHED` |

完成拼单的唯一 MVP 口径是通过 `POST /api/group-orders/{id}/pickup-status` 将 `pickupStatus` 推进到 `DISTRIBUTED`，后端同步将 `group_order.status` 更新为 `FINISHED` 并写入完成时间和状态日志。

## 6. 付款接口

### 6.1 成员标记已付款

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/group-orders/{id}/participants/{participantId}/payments/mark` |
| 关联流程 | 标记付款 |
| 权限说明 | 仅参与者本人可标记自己的付款状态。付款只表示线下付款后的状态标记，不接入真实支付。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 拼单 ID |
| path | `participantId` | number | 是 | 参与记录 ID |
| body | `remark` | string | 否 | 对应 `payment_record.remark` |

请求示例：

```json
{
  "remark": "已微信转账"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "participantId": 3001,
    "paymentStatus": "PAID",
    "paidMarkTime": "2026-05-27 18:40:00",
    "paymentRecord": {
      "id": 5001,
      "groupOrderId": 2001,
      "participantId": 3001,
      "userId": 1002,
      "amount": 18.76,
      "paymentStatus": "PAID",
      "markTime": "2026-05-27 18:40:00",
      "confirmUserId": null,
      "confirmTime": null,
      "remark": "已微信转账"
    }
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `拼单未锁定，暂不能标记付款` | 金额分摊尚未生成 |
| 403 | `只能标记自己的付款` | 当前用户不是参与者本人 |
| 404 | `参与记录不存在` | `participantId` 无效 |
| 409 | `付款已确认，不能重复标记` | 状态为 `CONFIRMED` |

### 6.2 发起人确认付款

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/group-orders/{id}/participants/{participantId}/payments/confirm` |
| 关联流程 | 发起人确认付款 |
| 权限说明 | 仅拼单发起人可确认成员付款。确认后 `order_participant.payment_status` 更新为 `CONFIRMED`，并追加 `payment_record`。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 拼单 ID |
| path | `participantId` | number | 是 | 参与记录 ID |
| body | `remark` | string | 否 | 确认备注 |

请求示例：

```json
{
  "remark": "金额已核对"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "participantId": 3001,
    "paymentStatus": "CONFIRMED",
    "paidConfirmTime": "2026-05-27 18:45:00",
    "paymentRecord": {
      "id": 5002,
      "groupOrderId": 2001,
      "participantId": 3001,
      "userId": 1002,
      "amount": 18.76,
      "paymentStatus": "CONFIRMED",
      "markTime": "2026-05-27 18:40:00",
      "confirmUserId": 1001,
      "confirmTime": "2026-05-27 18:45:00",
      "remark": "金额已核对"
    }
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `成员尚未标记付款` | 当前状态不是 `PAID` |
| 403 | `只有发起人可以确认付款` | 当前用户不是 `creator_id` |
| 404 | `参与记录不存在` | `participantId` 无效 |
| 409 | `付款已确认` | 重复确认 |

## 7. 取餐接口

### 7.1 指定取餐人

| 项 | 内容 |
| --- | --- |
| 方法 | `PUT` |
| 路径 | `/api/group-orders/{id}/pickup-assignee` |
| 关联流程 | 指定取餐人 |
| 权限说明 | 仅拼单发起人可指定。取餐人必须是当前拼单参与者。接口同时更新 `group_order.pickup_user_id`，并创建或更新 `pickup_record`。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 拼单 ID |
| body | `pickupUserId` | number | 是 | 取餐人用户 ID |
| body | `pickupLocation` | string | 否 | 实际取餐地点或履约阶段地点信息，对应 `pickup_record.pickup_location`；不传时沿用 `group_order.pickup_location` |
| body | `estimatedArrivalTime` | string | 否 | 预计到达时间 |
| body | `remark` | string | 否 | 取餐备注 |

请求示例：

```json
{
  "pickupUserId": 1002,
  "pickupLocation": "一教大厅门口",
  "estimatedArrivalTime": "2026-05-27 19:10:00",
  "remark": "小林去取"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 2001,
    "pickupUserId": 1002,
    "pickupRecord": {
      "id": 6001,
      "groupOrderId": 2001,
      "pickupUser": {
        "id": 1002,
        "username": "20260002",
        "nickname": "小林",
        "phone": null,
        "status": "ACTIVE"
      },
      "pickupLocation": "一教大厅门口",
      "pickupStatus": "WAITING_ORDER",
      "estimatedArrivalTime": "2026-05-27 19:10:00",
      "actualArrivalTime": null,
      "pickedUpTime": null,
      "distributedTime": null,
      "remark": "小林去取"
    }
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `取餐人必须是拼单参与者` | `pickupUserId` 未加入拼单 |
| 403 | `只有发起人可以指定取餐人` | 当前用户不是 `creator_id` |
| 409 | `拼单已取消或已完成` | 终态不能修改 |
| 404 | `用户不存在` | `pickupUserId` 无效 |

### 7.2 更新取餐状态

| 项 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/group-orders/{id}/pickup-status` |
| 关联流程 | 更新取餐状态、完成拼单 |
| 权限说明 | 发起人或指定取餐人可操作。取餐状态必须按 `WAITING_ORDER -> WAITING_DELIVERY -> ARRIVED -> PICKED_UP -> DISTRIBUTED` 合法流转，并与拼单主状态匹配；当 `pickupStatus = DISTRIBUTED` 时，拼单主状态同步为 `FINISHED`。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 拼单 ID |
| body | `pickupStatus` | string | 是 | 目标取餐状态 |
| body | `pickupLocation` | string | 否 | 实际取餐地点或履约阶段地点信息，对应 `pickup_record.pickup_location` |
| body | `actualArrivalTime` | string | 否 | 状态为 `ARRIVED` 时可提交 |
| body | `pickedUpTime` | string | 否 | 状态为 `PICKED_UP` 时可提交 |
| body | `distributedTime` | string | 否 | 状态为 `DISTRIBUTED` 时可提交 |
| body | `remark` | string | 否 | 取餐备注 |

请求示例：

```json
{
  "pickupStatus": "DISTRIBUTED",
  "pickupLocation": "宿舍楼下",
  "distributedTime": "2026-05-27 19:20:00",
  "remark": "已分发完成"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pickupRecord": {
      "id": 6001,
      "groupOrderId": 2001,
      "pickupUser": {
        "id": 1002,
        "username": "20260002",
        "nickname": "小林",
        "phone": null,
        "status": "ACTIVE"
      },
      "pickupLocation": "宿舍楼下",
      "pickupStatus": "DISTRIBUTED",
      "estimatedArrivalTime": "2026-05-27 19:10:00",
      "actualArrivalTime": "2026-05-27 19:05:00",
      "pickedUpTime": "2026-05-27 19:12:00",
      "distributedTime": "2026-05-27 19:20:00",
      "remark": "已分发完成"
    },
    "orderStatus": "FINISHED"
  }
}
```

说明：

- `pickupStatus = DISTRIBUTED` 表示取餐和分发已完成，是 MVP 阶段完成拼单的唯一接口口径。
- 后端在该状态推进成功后同步更新 `group_order.status = FINISHED`、写入 `finish_time` 和 `order_status_log`。
- MVP 阶段不使用 `PATCH /api/group-orders/{id}/status` 完成拼单。

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `未指定取餐人` | 没有 `pickup_record` |
| 400 | `取餐状态不能早于拼单状态` | 拼单尚未下单却标记到达 |
| 400 | `非法取餐状态流转` | 例如 `WAITING_DELIVERY -> DISTRIBUTED` |
| 403 | `无权更新取餐状态` | 非发起人且非取餐人 |
| 409 | `拼单已取消或已完成` | 终态不能更新 |

## 8. 我的拼单接口

### 8.1 查询我的拼单

| 项 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/my/group-orders` |
| 关联流程 | 我的拼单 |
| 权限说明 | 已登录用户。返回当前用户发起、参与和负责取餐的拼单。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| query | `scope` | string | 否 | `CREATED_BY_ME`、`JOINED_BY_ME`、`PICKUP_BY_ME`、`PENDING_PAYMENT`、`HISTORY`，不传返回当前用户相关全部拼单 |
| query | `status` | string | 否 | 拼单状态 |
| query | `pageNum` | number | 否 | 页码 |
| query | `pageSize` | number | 否 | 每页条数 |

`scope` 取值说明：

| scope | 说明 |
| --- | --- |
| `CREATED_BY_ME` | 当前用户发起的拼单 |
| `JOINED_BY_ME` | 当前用户作为参与者加入的拼单 |
| `PICKUP_BY_ME` | 当前用户负责取餐的拼单 |
| `PENDING_PAYMENT` | 当前用户待付款且拼单未结束的拼单 |
| `HISTORY` | 当前用户相关的已完成或已取消拼单 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "order": {
          "id": 2001,
          "title": "奶茶满 60 减 10",
          "orderType": "MILK_TEA",
          "merchantName": "一号门奶茶",
          "pickupLocation": "一教大厅门口",
          "deadlineTime": "2026-05-27 18:30:00",
          "participantCount": 3,
          "status": "LOCKED",
          "payableTotalAmount": 58.00,
          "pickupUser": {
            "id": 1002,
            "username": "20260002",
            "nickname": "小林",
            "phone": null,
            "status": "ACTIVE"
          }
        },
        "myRole": "PARTICIPANT",
        "myParticipantId": 3001,
        "myPayableAmount": 18.76,
        "myPaymentStatus": "PAID",
        "pickupStatus": "WAITING_ORDER"
      }
    ]
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `scope 不合法` | 范围枚举不存在 |
| 401 | `登录已失效` | 未登录 |

## 9. 数据看板接口

### 9.1 基础数据看板

| 项 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/dashboard/summary` |
| 关联流程 | 数据看板 |
| 权限说明 | 已登录用户可查看基础统计。MVP 可直接基于 MySQL 聚合查询实现，不依赖 Redis、RocketMQ 或异步统计。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| query | `startTime` | string | 否 | 统计开始时间，对应 `group_order.create_time` |
| query | `endTime` | string | 否 | 统计结束时间 |
| query | `scope` | string | 否 | `ALL`、`MINE`，默认 `ALL` |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayOrderCount": 3,
    "successOrderCount": 5,
    "totalSavedAmount": 90.00,
    "orderCount": 12,
    "createdCount": 3,
    "lockedCount": 2,
    "finishedCount": 5,
    "cancelledCount": 2,
    "participantCount": 36,
    "originalTotalAmount": 860.00,
    "actualDiscountAmount": 90.00,
    "payableTotalAmount": 770.00,
    "paidParticipantCount": 20,
    "confirmedParticipantCount": 18
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `startTime 不能晚于 endTime` | 时间范围非法 |
| 400 | `scope 不合法` | 范围枚举不存在 |
| 401 | `登录已失效` | 未登录 |

## 10. 增强接口与非强依赖说明

除认证模块 accessToken 黑名单外，通知记录、Redis 和 RocketMQ 均为增强能力，不得作为拼单业务主流程硬依赖。拼单大厅、发起拼单、加入拼单、锁单分摊、付款标记、取餐状态和完成拼单必须在仅依赖 MySQL 的情况下可运行；认证模块的 logout 后 accessToken 立即失效依赖 Redis 黑名单。

### 10.1 通知列表（增强）

| 项 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/notifications` |
| 关联流程 | 通知提醒增强 |
| 权限说明 | 已登录用户只能查看自己的通知，对应 `notification_record.receiver_id`。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| query | `status` | string | 否 | `UNREAD`、`READ` |
| query | `pageNum` | number | 否 | 页码 |
| query | `pageSize` | number | 否 | 每页条数 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "records": [
      {
        "id": 7001,
        "receiverId": 1002,
        "bizType": "PICKUP",
        "bizId": 2001,
        "title": "取餐状态更新",
        "content": "奶茶拼单已到达",
        "status": "UNREAD",
        "readTime": null,
        "createTime": "2026-05-27 19:05:00"
      }
    ]
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 400 | `status 不合法` | 通知状态非法 |
| 401 | `登录已失效` | 未登录 |

### 10.2 标记通知已读（增强）

| 项 | 内容 |
| --- | --- |
| 方法 | `PATCH` |
| 路径 | `/api/notifications/{id}/read` |
| 关联流程 | 通知提醒增强 |
| 权限说明 | 已登录用户只能标记自己的通知。 |

请求参数：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| path | `id` | number | 是 | 通知 ID，对应 `notification_record.id` |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 7001,
    "status": "READ",
    "readTime": "2026-05-27 19:08:00"
  }
}
```

错误场景：

| code | message | 说明 |
| --- | --- | --- |
| 403 | `无权操作该通知` | `receiver_id` 不是当前用户 |
| 404 | `通知不存在` | 通知 ID 无效 |

### 10.3 Redis 与 RocketMQ 边界

- Redis 对拼单大厅缓存、热门详情缓存、防重复提交、短时互斥和限流仍属于增强能力，但认证模块 accessToken 黑名单是必选能力。
- accessToken 黑名单 Redis key 固定为 `auth:blacklist:access:{tokenHash}`，`tokenHash` 为 accessToken 的 SHA-256 摘要，TTL 为 accessToken 剩余有效期。
- Redis 中不得存储明文 accessToken 或 refreshToken。
- RocketMQ 可增强截止过期处理、状态变更通知和异步统计，但消息失败不能阻断拼单主流程。
- MVP 阶段不使用 RocketMQ 自动生成 `EXPIRED` 拼单状态；超时不继续拼单时由发起人手动取消为 `CANCELLED`。
- 除认证黑名单外，MVP 不定义 Redis 或 RocketMQ 的强制业务接口；如后续增加管理或重试接口，需先更新本文档和任务看板。

## 11. MVP 主流程接口覆盖关系

| 主流程节点 | 对应接口 |
| --- | --- |
| 登录与会话 | `POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/refresh`、`POST /api/auth/logout` |
| 用户注册（前置能力） | `POST /api/auth/register` |
| 发起拼单 | `POST /api/group-orders` |
| 成员加入 | `POST /api/group-orders/{id}/participants` |
| 凑单计算 | `GET /api/group-orders`、`GET /api/group-orders/{id}`、加入拼单响应中的金额汇总 |
| 锁定拼单 | `POST /api/group-orders/{id}/lock` |
| 优惠分摊 | `POST /api/group-orders/{id}/lock`、`GET /api/group-orders/{id}` |
| 标记付款 | `POST /api/group-orders/{id}/participants/{participantId}/payments/mark` |
| 发起人确认付款 | `POST /api/group-orders/{id}/participants/{participantId}/payments/confirm` |
| 指定取餐人 | `PUT /api/group-orders/{id}/pickup-assignee` |
| 更新取餐状态 | `POST /api/group-orders/{id}/pickup-status` |
| 完成拼单 | `POST /api/group-orders/{id}/pickup-status`，`pickupStatus = DISTRIBUTED` 后同步 `group_order.status = FINISHED` |
| 我的拼单 | `GET /api/my/group-orders` |
| 数据看板 | `GET /api/dashboard/summary` |
