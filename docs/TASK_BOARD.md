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
| T011 | 实现锁单与优惠分摊 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T010-BE | 已实现 POST /api/group-orders/{orderId}/lock，仅发起人可在 CREATED 状态锁单；锁单时基于 meal_item.subtotal_amount 重新汇总金额，按成员原始金额占比分摊优惠，尾差固定写入发起人 rounding_adjustment_amount，并更新 group_order 汇总、LOCKED 状态、locked_time 和 order_status_log；已通过 mvn test、mvn -DskipTests package |
| T012 | 已实现后端模块测试与审查 | 测试会话 | DONE | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T008, T009, T010-BE, T011 | 已补充认证、拼单大厅、发起拼单、详情、加入、锁单与优惠分摊 Service 单元测试；原记录付款接口未实现，现已由 T012-BE 实现；已通过 mvn test |
| T012-BE | 实现付款标记与付款确认 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T011 | 已实现 POST /api/group-orders/{orderId}/participants/{participantId}/payments/mark 与 /confirm；仅 LOCKED、ORDERED、DELIVERING、ARRIVED 可操作，成员只能标记自己付款，发起人可确认任意成员付款，重复标记/确认返回明确业务错误；已写入 payment_record 并通过 mvn test、mvn -DskipTests package |
| T012-QA | T008-T012 后端模块测试与审查 | 测试会话 | DONE | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T008, T009, T010-BE, T011, T012-BE | 已补充认证、拼单、锁单分摊、付款标记与付款确认测试；曾记录大厅默认排序契约差异（BUG-QA-001，已由后端修复）和 EXPIRED 状态口径差异；已通过 mvn test（30 个测试） |
| BUG-QA-001 | 修复拼单大厅默认排序 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T012-QA | 已修复 GET /api/group-orders 未指定 status 时的默认排序：未结束拼单优先，同一优先级内按 create_time desc；不改变接口路径和响应结构，不处理 EXPIRED 状态；已通过 mvn test、mvn -DskipTests package |
| BUG-QA-002 | 收口 EXPIRED 状态口径 | 主控会话 | DONE | docs/02-领域模型与业务规则.md, docs/04-API接口文档.md, docs/05-测试用例.md, docs/TASK_BOARD.md | T012-QA | 已按“延后 EXPIRED”处理：MVP 不实现 EXPIRED，超时不继续拼单由发起人手动取消为 CANCELLED，RocketMQ 自动过期作为增强功能后置；未修改后端代码 |
| T013 | 实现取餐状态更新与完成拼单 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T012-BE | 已实现 PUT /api/group-orders/{id}/pickup-assignee 与 PATCH /api/group-orders/{id}/pickup-status；支持发起人指定取餐人、发起人或取餐人按顺序更新取餐状态，取餐记录、拼单主状态与 order_status_log 同步更新；已通过 mvn test（34 个测试）和 mvn -DskipTests package |
| T013-QA | T013 取餐状态更新与完成拼单测试 | 测试会话 | DONE | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T013 | 已补充指定取餐人权限、取餐人归属、发起人/取餐人更新状态、普通参与者拒绝、取餐状态顺序推进、主状态同步、FINISHED 后不可修改和 order_status_log 断言；已通过 mvn test（39 个测试） |
| T014 | 实现我的拼单与数据看板 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T013 | 已实现 GET /api/my/group-orders 与 GET /api/dashboard/summary；我的拼单支持 CREATED_BY_ME、JOINED_BY_ME、PICKUP_BY_ME、PENDING_PAYMENT、HISTORY，数据看板基于当前登录用户相关拼单聚合今日拼单数、成功拼单数、累计节省金额、热门类型/店铺等演示统计；未引入 Redis/MQ；已通过 mvn test（42 个测试）和 mvn -DskipTests package |
| T014-QA | T014 我的拼单与数据看板测试 | 测试会话 | DONE | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T014 | 已补充我的发起、我的参与、待付款、历史拼单、无关用户过滤和数据看板今日拼单数/成功拼单数/累计节省金额测试；记录 API 文档 scope 与看板响应示例需补齐；已通过 mvn test（46 个测试） |
| T015-AUTH-DESIGN | 升级认证模块设计 | 主控会话 | DONE | docs/03-数据库设计.md, docs/04-API接口文档.md, docs/TASK_BOARD.md | T008, T008-API | 已将认证契约升级为 accessToken + refreshToken，新增 refresh/logout API，明确 token hash 存储，不引入 OAuth2、短信验证码或复杂 RBAC |
| T015-SQL | 同步认证升级数据库脚本 | SQL 会话 | DONE | docs/03-数据库设计.md, sql/schema.sql, sql/data.sql, docs/TASK_BOARD.md | T015-AUTH-DESIGN | 已新增 user.last_login_time、user.last_login_ip、user.password_update_time 和 user_refresh_token 表；refreshToken 不明文落库，只保存 token_hash，并已更新演示数据 |
| T015-BE | 实现双 token 认证接口 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T015-SQL | 已实现 login 返回 accessToken、refreshToken、expiresIn，新增 refresh/logout；refreshToken 仅保存 token_hash，refresh 成功轮换 refreshToken，logout 作废 refreshToken 并将 accessToken hash 加入内存黑名单；登录更新 last_login_time、last_login_ip；已通过 mvn test（49 个测试）和 mvn -DskipTests package |
| T015-QA | 回归认证升级接口 | 测试会话 | TODO | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T015-BE | 覆盖登录、me、refresh、logout、黑名单访问拒绝、refreshToken 作废和禁用用户场景 |

## 使用规则

- 开始任务前将状态改为 `DOING`。
- 完成并验证后将状态改为 `DONE`。
- 无法继续时将状态改为 `BLOCKED`，并在备注中说明原因。
- 不删除历史任务；废弃任务在备注中说明。
