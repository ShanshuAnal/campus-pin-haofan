# backend/AGENTS.md

## 适用范围

本文件适用于后端会话和任何修改 `backend/` 的任务。不要在本目录下生成前端页面、SQL 表结构脚本或文档汇报材料。

## 技术栈

- Java 21
- Spring Boot 3.2.x
- MyBatis-Plus
- MySQL 8
- Maven
- Lombok
- Validation
- RESTful API

认证方案：

- Spring Security 可选。
- MVP 阶段可先使用简化 JWT 认证。
- 不接入真实支付、短信、地图定位或外卖平台接口。

## Redis 与 RocketMQ 增强边界

Redis 和 RocketMQ 只能作为增强能力，不能作为 MVP 主流程硬依赖。

允许使用 Redis 的场景：

- 拼单大厅列表缓存；
- 热门拼单详情缓存；
- 登录 token/session 缓存；
- 防重复提交；
- 短时互斥锁；
- 简单限流。

Redis 禁止作为订单、付款、金额分摊和取餐状态的唯一存储。MySQL 始终是最终数据源。

允许使用 RocketMQ 的场景：

- 拼单截止后的异步过期处理；
- 状态变更通知；
- 异步统计；
- 低优先级日志或事件处理。

RocketMQ 禁止替代同步业务校验。消息消费必须幂等，消息失败必须可重试或可人工补偿。

新增 Redis 或 RocketMQ 前，必须说明使用场景、解决的问题、影响范围、配置项和不可用时的降级方案。

## 包结构

后端基础包名建议为：

```text
com.campus.pinhaofan
```

建议结构：

```text
backend/src/main/java/com/campus/pinhaofan/
├── CampusPinHaofanApplication.java
├── common/
│   ├── config/
│   ├── exception/
│   ├── response/
│   └── util/
├── module/
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── service/impl/
│   │   ├── mapper/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── vo/
│   ├── grouporder/
│   ├── payment/
│   └── pickup/
└── security/
```

## 分层职责

- Controller 只处理参数接收、基础校验和响应返回。
- Service 承载业务流程、金额计算、权限判断和状态流转校验。
- Mapper 仅负责数据库访问，不写业务判断。
- Entity 对应数据库表结构。
- DTO 用于请求参数。
- VO 用于响应视图。
- 公共响应、异常处理、分页、时间处理等放入 `common`。
- 认证授权相关能力放入 `security`。

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 金额计算规则

- 所有金额计算必须使用 `BigDecimal`。
- 禁止使用 `double` 或 `float` 处理金额。
- 数据库金额字段遵守 `sql/AGENTS.md`。
- 拼单总金额 = 所有参与者原始金额之和。
- 优惠金额按照参与者原始金额占比分摊。
- 实际应付金额 = 原始金额 - 个人分摊优惠。
- 金额统一保留两位小数。
- 分摊规则必须保证总分摊金额与订单总金额一致。
- 小数误差由拼单发起人承担或补齐。
- 单元测试必须覆盖金额分摊、优惠分摊和尾差场景。

## 状态流转校验

拼单状态：

```text
CREATED      进行中，允许加入
LOCKED       已锁单，不允许继续加入
ORDERED      已下单
DELIVERING   配送中
ARRIVED      已到达
PICKED_UP    已取餐
FINISHED     已完成
CANCELLED    已取消
```

付款状态：

```text
UNPAID       未付款
PAID         已付款
CONFIRMED    发起人已确认
REFUNDED     已退款
```

取餐状态：

```text
WAITING_ORDER      待下单
WAITING_DELIVERY   待配送
ARRIVED            已到达
PICKED_UP          已取餐
DISTRIBUTED        已分发
```

要求：

- 每次状态变更必须校验当前状态、目标状态、操作者权限和必要业务条件。
- 不允许从 `FINISHED` 回退。
- 不允许从 `CANCELLED` 继续推进。
- 不允许绕过锁单直接进入完成状态。
- 状态枚举和流转逻辑应集中维护，避免散落在多个 Controller 中。
- 不允许仅依赖前端按钮控制业务状态。

## 关键业务约束

- 只有 `CREATED` 状态的拼单允许加入。
- 同一个用户不能重复加入同一个拼单。
- 拼单人数不能超过最大人数限制。
- 点餐金额必须大于 0。
- `LOCKED` 之后不能继续加入或修改点餐内容。
- `FINISHED` 和 `CANCELLED` 状态不能继续修改。
- 只有拼单发起人可以锁单、取消拼单、更新主要履约状态。
- 付款状态只做模拟标记，不接入真实支付。
- 发起人确认付款前，参与者的付款标记不能视为最终确认。
- 取餐状态更新必须校验操作者权限和当前拼单状态。

## 后端命令

后端工程创建后，优先使用以下命令：

```powershell
cd backend
mvn spring-boot:run
mvn test
mvn -DskipTests package
```

如项目后续引入 Maven Wrapper，可改用：

```powershell
cd backend
.\mvnw spring-boot:run
.\mvnw test
.\mvnw -DskipTests package
```

未创建后端工程前，不要为了执行命令而生成 `pom.xml` 或业务代码。
