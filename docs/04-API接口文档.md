# 校园拼好饭系统 V2 API 接口文档

## 1. 文档定位

本文是前后端接口唯一契约。后端、前端和测试会话修改接口前，必须先更新本文。

V2 API 设计保留当前 MVP 已完成能力，并新增取消拼单、超时关闭、履约事件、并发保护和产品化前端所需的数据结构。

## 2. 接口原则

- 拼单相关 API 统一使用 `/api/group-orders`。
- 不使用 `/api/orders` 表示拼单资源。
- API 对外用户账号字段统一使用 `username`，映射数据库 `user.account`。
- 金额字段使用字符串或两位小数数值表达，后端内部必须使用 `BigDecimal`。
- 状态字段统一使用大写枚举字符串。
- 核心状态以 MySQL 为准。
- Redis 可用于黑名单、防重复提交、短时锁和缓存。
- RocketMQ 只用于异步通知、超时检查和统计聚合，不作为核心状态源。

## 3. 通用规范

### 3.1 请求头

除注册、登录、刷新 token 外，其他接口默认需要登录。

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

建议写操作支持防重复提交头：

```http
Idempotency-Key: {clientGeneratedKey}
```

后端可将防重复提交 key 映射到 Redis：

```text
idem:{userId}:{method}:{path}:{bizKey}
```

### 3.2 通用响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

通用错误：

| code | 含义 |
| --- | --- |
| `400` | 请求参数错误 |
| `401` | 未登录或 token 无效 |
| `403` | 无操作权限 |
| `404` | 资源不存在 |
| `409` | 状态冲突或重复操作 |
| `500` | 服务端错误 |

## 4. 状态枚举

### 4.1 拼单主状态

```text
CREATED, LOCKED, ORDERED, DELIVERING, ARRIVED, PICKED_UP,
FINISHED, CANCELLED, EXPIRED
```

说明：

- `CANCELLED`：发起人主动取消；
- `EXPIRED`：系统根据截止时间自动关闭；
- `FINISHED`、`CANCELLED`、`EXPIRED` 为终态；
- 延迟和异常不新增主状态，使用事件接口记录。

### 4.2 付款状态

```text
UNPAID, PAID, CONFIRMED, REFUNDED
```

### 4.3 取餐状态

```text
WAITING_ORDER, WAITING_DELIVERY, ARRIVED, PICKED_UP, DISTRIBUTED
```

当取餐状态推进到 `DISTRIBUTED` 后，拼单主状态同步为 `FINISHED`。

### 4.4 事件类型

```text
CANCELLED, EXPIRED, DELAY_REPORTED, MERCHANT_DELAY, DELIVERY_DELAY,
PICKUP_EXCEPTION, ITEM_MISSING, CONTACT_FAILED, PAYMENT_DISPUTE, NOTE
```

## 5. 认证接口

### 5.1 注册

```http
POST /api/auth/register
```

请求：

```json
{
  "username": "student001",
  "password": "P@ssw0rd123",
  "nickname": "张三",
  "phone": "13800000000"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "student001",
    "nickname": "张三",
    "phone": "13800000000",
    "status": "NORMAL"
  }
}
```

错误场景：

- `409`：账号已存在；
- `400`：密码强度或参数格式不符合要求。

### 5.2 登录

```http
POST /api/auth/login
```

请求：

```json
{
  "username": "student001",
  "password": "P@ssw0rd123"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "access-token",
    "refreshToken": "refresh-token",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "student001",
      "nickname": "张三",
      "avatarUrl": null
    }
  }
}
```

规则：

- token 不明文落库；
- refreshToken 只保存 SHA-256 摘要；
- accessToken 黑名单存储在 Redis。

### 5.3 获取当前用户

```http
GET /api/auth/me
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "student001",
    "nickname": "张三",
    "phone": "13800000000",
    "avatarUrl": null,
    "status": "NORMAL"
  }
}
```

### 5.4 刷新 token

```http
POST /api/auth/refresh
```

请求：

```json
{
  "refreshToken": "refresh-token"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token",
    "expiresIn": 7200
  }
}
```

### 5.5 登出

```http
POST /api/auth/logout
```

请求：

```json
{
  "refreshToken": "refresh-token"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

存储规则：

- logout 后 accessToken SHA-256 摘要写入 Redis；
- Redis key：`auth:blacklist:access:{tokenHash}`；
- TTL：accessToken 剩余有效期；
- refreshToken 在 `user_refresh_token` 中作废。

## 6. 拼单大厅

### 6.1 查询拼单大厅

```http
GET /api/group-orders
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `status` | 否 | 默认查询可加入和进行中的拼单 |
| `keyword` | 否 | 店铺或标题关键字 |
| `pickupLocation` | 否 | 取餐地点筛选 |
| `onlyJoinable` | 否 | 是否只看可加入 |
| `page` | 否 | 页码 |
| `size` | 否 | 每页条数 |

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1001,
        "title": "二食堂麻辣香锅拼单",
        "shopName": "二食堂麻辣香锅",
        "pickupLocation": "宿舍区东门",
        "deadlineTime": "2026-05-29T18:30:00",
        "minAmount": "60.00",
        "totalAmount": "42.00",
        "discountAmount": "8.00",
        "status": "CREATED",
        "joinable": true,
        "participantCount": 3,
        "progressPercent": 70,
        "remainingSeconds": 900,
        "lastEventSummary": null
      }
    ],
    "total": 1
  }
}
```

前端产品化要求：

- 拼单大厅优先呈现可加入拼单；
- 卡片需要突出店铺、取餐点、截止时间、凑单进度和成员数；
- 不按管理后台表格作为主要体验。

## 7. 拼单详情

### 7.1 获取拼单详情

```http
GET /api/group-orders/{id}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1001,
    "title": "二食堂麻辣香锅拼单",
    "shopName": "二食堂麻辣香锅",
    "pickupLocation": "宿舍区东门",
    "deadlineTime": "2026-05-29T18:30:00",
    "minAmount": "60.00",
    "totalAmount": "80.00",
    "discountAmount": "10.00",
    "status": "LOCKED",
    "creator": {
      "id": 1,
      "nickname": "张三"
    },
    "pickupUser": {
      "id": 2,
      "nickname": "李四"
    },
    "currentUserRole": "PARTICIPANT",
    "permissions": {
      "canJoin": false,
      "canLock": false,
      "canCancel": false,
      "canMarkPayment": true,
      "canConfirmPayment": false,
      "canUpdatePickupStatus": false,
      "canCreateEvent": false
    },
    "participants": [
      {
        "id": 501,
        "userId": 2,
        "nickname": "李四",
        "originalAmount": "28.00",
        "shareDiscountAmount": "3.50",
        "payableAmount": "24.50",
        "paymentStatus": "UNPAID",
        "items": [
          {
            "itemName": "牛肉饭",
            "unitPrice": "28.00",
            "quantity": 1,
            "subtotalAmount": "28.00"
          }
        ]
      }
    ],
    "pickup": {
      "pickupStatus": "WAITING_ORDER",
      "pickupLocation": "宿舍区东门"
    },
    "recentEvents": []
  }
}
```

前端产品化要求：

- 详情页应按协同流程组织信息；
- 顶部展示状态、倒计时、取餐点和核心操作；
- 中部展示成员点单与金额分摊；
- 底部展示付款、取餐和事件时间线。

## 8. 创建与加入拼单

### 8.1 发起拼单

```http
POST /api/group-orders
```

请求：

```json
{
  "title": "二食堂麻辣香锅拼单",
  "shopName": "二食堂麻辣香锅",
  "pickupLocation": "宿舍区东门",
  "deadlineTime": "2026-05-29T18:30:00",
  "minAmount": "60.00",
  "discountAmount": "10.00",
  "remark": "满 60 减 10"
}
```

响应：返回拼单详情。

### 8.2 加入拼单

```http
POST /api/group-orders/{id}/participants
```

请求：

```json
{
  "remark": "少辣",
  "items": [
    {
      "itemName": "牛肉饭",
      "unitPrice": "28.00",
      "quantity": 1
    }
  ]
}
```

规则：

- 只有 `CREATED` 状态可加入；
- 同一用户不能重复加入同一拼单；
- 后端必须重新计算成员原始金额和拼单总金额；
- 并发加入以 MySQL 唯一约束和事务为准。

错误场景：

- `409`：拼单已锁定、取消、过期或用户已加入。

## 9. 锁定、下单与取餐

### 9.1 锁定拼单

```http
POST /api/group-orders/{id}/lock
```

权限：发起人。

规则：

- 仅 `CREATED` 可锁定；
- 锁定时计算优惠分摊；
- 锁定后不能加入或修改餐品；
- 状态变为 `LOCKED`。

### 9.2 标记已下单

```http
POST /api/group-orders/{id}/ordered
```

权限：发起人。

规则：

- 仅 `LOCKED` 可操作；
- 表示发起人已在线下渠道完成实际下单；
- 状态变为 `ORDERED`；
- 取餐状态进入 `WAITING_DELIVERY`。

### 9.3 指定取餐人

```http
POST /api/group-orders/{id}/pickup-user
```

请求：

```json
{
  "pickupUserId": 2,
  "pickupLocation": "宿舍区东门"
}
```

权限：发起人。

规则：

- 取餐人必须是拼单成员或发起人；
- 可在 `LOCKED`、`ORDERED` 状态指定；
- 已完成、取消或过期后不可修改。

### 9.4 推进取餐状态

```http
POST /api/group-orders/{id}/pickup-status
```

请求：

```json
{
  "pickupStatus": "ARRIVED",
  "pickupLocation": "宿舍区东门",
  "remark": "已到东门"
}
```

权限：发起人或取餐人。

规则：

- 取餐状态必须按顺序推进；
- `ARRIVED` 可同步拼单主状态为 `ARRIVED`；
- `PICKED_UP` 可同步拼单主状态为 `PICKED_UP`；
- `DISTRIBUTED` 必须同步拼单主状态为 `FINISHED`；
- MVP/V2 均不再使用 `PATCH /api/group-orders/{id}/status` 作为完成拼单接口。

## 10. 付款协同

### 10.1 标记付款

```http
POST /api/group-orders/{id}/payments
```

请求：

```json
{
  "amount": "24.50",
  "remark": "微信已转"
}
```

权限：拼单参与成员。

规则：

- 成员只能标记自己的付款；
- 状态变为 `PAID`；
- 系统不接入真实支付。

### 10.2 确认付款

```http
POST /api/group-orders/{id}/payments/{participantId}/confirm
```

权限：发起人。

规则：

- 发起人确认后付款状态为 `CONFIRMED`；
- 付款争议通过事件接口记录。

## 11. 取消与超时

### 11.1 取消拼单

```http
POST /api/group-orders/{id}/cancel
```

V2 必做。

请求：

```json
{
  "cancelReason": "人数不够，先取消"
}
```

权限：发起人。

允许状态：

```text
CREATED, LOCKED, ORDERED
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1001,
    "status": "CANCELLED",
    "cancelReason": "人数不够，先取消",
    "cancelTime": "2026-05-29T18:20:00"
  }
}
```

规则：

- 取消后不能加入、锁定、付款或推进取餐；
- 已付款成员的退款由线下协商处理；
- 系统写入 `GroupOrderEvent` 和状态日志。

### 11.2 超时自动关闭

V2 必做，推荐由 RocketMQ 延迟消息或定时任务触发服务层方法。

不建议提供给普通前端调用的公开接口。为测试和内部运维可保留内部接口：

```http
POST /api/internal/group-orders/expire-check
```

权限：系统内部。

规则：

- 仅 `CREATED` 且超过 `deadlineTime` 的拼单可变更为 `EXPIRED`；
- 已锁定、已取消、已完成的拼单不再过期；
- 处理前必须重新读取 MySQL；
- MQ 重复消息必须幂等；
- 系统写入 `GroupOrderEvent` 和状态日志。

## 12. 履约事件

### 12.1 新增事件

```http
POST /api/group-orders/{id}/events
```

V2 必做。

请求：

```json
{
  "eventType": "DELAY_REPORTED",
  "eventLevel": "WARN",
  "title": "商家出餐延迟",
  "content": "预计晚到 10 分钟"
}
```

权限：

- 发起人；
- 取餐人；
- 部分普通备注可允许参与成员提交，具体以后端权限策略为准。

规则：

- 延迟和异常事件不直接改变主状态；
- 终态后可追加备注类事件，但不能修改主流程状态；
- 事件只追加，不覆盖历史。

### 12.2 查询事件

```http
GET /api/group-orders/{id}/events
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 9001,
      "eventType": "DELAY_REPORTED",
      "eventLevel": "WARN",
      "operatorId": 2,
      "operatorRole": "PICKUP_USER",
      "title": "商家出餐延迟",
      "content": "预计晚到 10 分钟",
      "eventTime": "2026-05-29T18:45:00"
    }
  ]
}
```

## 13. 我的拼单

### 13.1 查询我的拼单

```http
GET /api/group-orders/my
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `scope` | `CREATED_BY_ME`、`JOINED`、`PICKUP`、`PENDING_PAYMENT`、`HISTORY` |
| `page` | 页码 |
| `size` | 每页条数 |

规则：

- `PENDING_PAYMENT` 返回当前用户待付款或未确认付款的拼单；
- `HISTORY` 返回 `FINISHED`、`CANCELLED`、`EXPIRED` 等终态拼单；
- 前端应以产品化列表展示，不使用后台式数据表作为主要视图。

## 14. 数据看板

### 14.1 查询个人看板

```http
GET /api/dashboard/summary
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "todayOrderCount": 3,
    "successOrderCount": 12,
    "totalSavedAmount": "36.50",
    "pendingPaymentCount": 1,
    "pickupTaskCount": 2
  }
}
```

## 15. 异步通知

### 15.1 查询通知

```http
GET /api/notifications
```

V2 可选增强。

说明：

- 通知不影响主流程；
- 通知失败不回滚拼单状态；
- RocketMQ 可用于异步生成通知记录。

## 16. V2 接口实现优先级

| 能力 | 接口 | 优先级 |
| --- | --- | --- |
| 注册、登录、当前用户、刷新、登出 | `/api/auth/**` | MVP 保留 |
| 拼单大厅、详情、创建、加入 | `/api/group-orders/**` | MVP 保留 |
| 锁定、付款、取餐 | `/api/group-orders/**` | MVP 保留 |
| 取消拼单 | `POST /api/group-orders/{id}/cancel` | V2 必做 |
| 超时关闭 | 内部 expire check 或 MQ 消费 | V2 必做 |
| 履约事件 | `/api/group-orders/{id}/events` | V2 必做 |
| 并发保护 | 写接口状态校验和幂等头 | V2 必做 |
| Redis 防重复提交和缓存 | 写接口和查询接口增强 | V2 可选增强 |
| RocketMQ 通知和统计 | 异步增强 | V2 可选增强 |
