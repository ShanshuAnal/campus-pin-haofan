# 任务看板

本文件用于多会话并行开发时记录任务状态。不要提前填充大量虚构任务，只登记已经明确分配或准备执行的任务。

## 状态说明

- TODO：待开始。
- DOING：进行中。
- DONE：已完成并验证。
- BLOCKED：被依赖、冲突或环境问题阻塞。

## 看板模板

| 任务 ID | 任务名称 | 负责会话 | 状态 | 涉及目录 | 依赖任务 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| T001 | 编写需求说明文档 | 主控会话 | DONE | docs/01-需求说明.md | 无 | 已创建需求说明文档并完成内容检查 |
| T002 | 编写领域模型与业务规则文档 | 主控会话 | DONE | docs/02-领域模型与业务规则.md | T001 | 已创建领域模型、状态流转、金额规则和异常场景说明 |
| T003 | 编写数据库设计文档 | SQL 设计会话 | DONE | docs/03-数据库设计.md | T002 | 已完成 MySQL 8 表结构设计说明，暂不生成 schema.sql |
| T004 | 生成 schema.sql 和 data.sql | SQL 会话 | DONE | sql/schema.sql, sql/data.sql | T003 | 已根据数据库设计生成 MySQL 8 初始化脚本和演示数据 |
| T004.5 | SQL 脚本执行验证 | SQL 会话 | DONE | docs/03-数据库设计.md | T004 | 已记录 schema.sql 和 data.sql 在 MySQL 8.4.9 的 campus_haofan 数据库执行通过 |
| T005 | 生成 API 接口文档 | API 设计会话 | DONE | docs/04-API接口文档.md, docs/TASK_BOARD.md | T003, T004.5 | 已生成覆盖 MVP 主流程的 API 契约，字段与数据库设计保持一致，Redis、RocketMQ 和通知记录仅作为增强能力 |
| T006 | 创建后端工程骨架 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T005 | 已创建 Java 21 + Spring Boot 3.2.x + Maven 后端工程骨架，并通过 mvn test、mvn -DskipTests package 和启动验证；暂未实现具体业务接口 |
| T007 | 生成后端实体、Mapper 和状态枚举 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T006 | 已根据 schema.sql 生成 8 张表对应 entity、Mapper 和状态枚举，并通过 mvn test、mvn -DskipTests package；未实现业务接口 |
| T008 | 实现用户认证模块 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T007, T008-API | 已实现 register/login/me 认证接口，username 映射 user.account，密码 BCrypt 存入 password_hash，轻量 token 解析 Authorization；已通过 mvn test、mvn -DskipTests package |
| T008-API | 补齐认证接口契约 | API 设计会话 | DONE | docs/04-API接口文档.md, docs/TASK_BOARD.md, sql/schema.sql, sql/data.sql | T005 | 已统一为 MVP 轻量真实登录，补齐 register/login/me 契约，username 映射 user.account，password_hash 已在数据库设计和 SQL 中同步 |
| T009 | 实现拼单大厅查询和发起拼单 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T008, T010 | 已实现 GET/POST /api/group-orders，支持状态、类型、关键词筛选，创建时使用当前用户为 creator_id、默认 CREATED 并写入 order_status_log；已通过 mvn test、mvn -DskipTests package |
| T010 | 全局契约对齐 | 主控会话 | DONE | docs/03-数据库设计.md, docs/04-API接口文档.md, sql/schema.sql, sql/data.sql, docs/CONTRACT_ALIGNMENT_REPORT.md | T008, T009 | 已对齐 API 路径、username/account 映射、pickup_location、增强表和中间件边界；后端拼单详情与加入拼单实现见 T010-BE |
| T010-BE | 实现拼单详情与加入拼单 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T009 | 已实现 GET /api/group-orders/{orderId} 与 POST /api/group-orders/{orderId}/participants，详情返回参与者、餐品、金额进度和取餐状态；加入拼单会校验 CREATED、截止时间、重复加入、人数上限和餐品金额，并更新订单金额汇总；已通过 mvn test、mvn -DskipTests package |

## 使用规则

- 开始任务前将状态改为 `DOING`。
- 完成并验证后将状态改为 `DONE`。
- 无法继续时将状态改为 `BLOCKED`，并在备注中说明原因。
- 不删除历史任务；废弃任务在备注中说明。
