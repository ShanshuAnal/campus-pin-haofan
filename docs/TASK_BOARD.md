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
| T013 | 实现取餐状态更新与完成拼单 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T012-BE | 已实现 PUT /api/group-orders/{id}/pickup-assignee 与取餐状态推进接口；当前 API 契约由 T017-API-CONTRACT 统一为 POST /api/group-orders/{id}/pickup-status；支持发起人指定取餐人、发起人或取餐人按顺序更新取餐状态，取餐记录、拼单主状态与 order_status_log 同步更新；已通过 mvn test（34 个测试）和 mvn -DskipTests package |
| T013-QA | T013 取餐状态更新与完成拼单测试 | 测试会话 | DONE | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T013 | 已补充指定取餐人权限、取餐人归属、发起人/取餐人更新状态、普通参与者拒绝、取餐状态顺序推进、主状态同步、FINISHED 后不可修改和 order_status_log 断言；已通过 mvn test（39 个测试） |
| T014 | 实现我的拼单与数据看板 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T013 | 已实现 GET /api/my/group-orders 与 GET /api/dashboard/summary；我的拼单支持 CREATED_BY_ME、JOINED_BY_ME、PICKUP_BY_ME、PENDING_PAYMENT、HISTORY，数据看板基于当前登录用户相关拼单聚合今日拼单数、成功拼单数、累计节省金额、热门类型/店铺等演示统计；未引入 Redis/MQ；已通过 mvn test（42 个测试）和 mvn -DskipTests package |
| T014-QA | T014 我的拼单与数据看板测试 | 测试会话 | DONE | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T014 | 已补充我的发起、我的参与、待付款、历史拼单、无关用户过滤和数据看板今日拼单数/成功拼单数/累计节省金额测试；记录 API 文档 scope 与看板响应示例需补齐；已通过 mvn test（46 个测试） |
| T015-AUTH-DESIGN | 升级认证模块设计 | 主控会话 | DONE | docs/03-数据库设计.md, docs/04-API接口文档.md, docs/TASK_BOARD.md | T008, T008-API | 已将认证契约升级为 accessToken + refreshToken，新增 refresh/logout API，明确 token 只保存 SHA-256 摘要，不引入 OAuth2、短信验证码或复杂 RBAC |
| T015-SQL | 同步认证升级数据库脚本 | SQL 会话 | DONE | docs/03-数据库设计.md, sql/schema.sql, sql/data.sql, docs/TASK_BOARD.md | T015-AUTH-DESIGN | 已新增 user.last_login_time、user.last_login_ip、user.password_update_time 和 user_refresh_token 表；refreshToken 不明文落库，只保存 SHA-256 摘要到 token_hash，并已更新演示数据 |
| T015-BE | 实现双 token 认证接口 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T015-SQL | 已实现 login 返回 accessToken、refreshToken、expiresIn，新增 refresh/logout；refreshToken 仅保存 SHA-256 摘要到 token_hash，refresh 成功轮换 refreshToken，logout 作废 refreshToken 并接入 accessToken 黑名单；黑名单存储口径已由 T015-AUTH-REDIS-DESIGN 收口为 Redis 必选；登录更新 last_login_time、last_login_ip；已通过 mvn test（49 个测试）和 mvn -DskipTests package |
| T015-QA | 回归认证升级接口 | 测试会话 | TODO | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | T015-BE | 覆盖登录、me、refresh、logout、黑名单访问拒绝、refreshToken 作废和禁用用户场景 |
| T015-AUTH-REDIS-DESIGN | 对齐认证 Redis 黑名单口径 | 主控会话 | DONE | docs/03-数据库设计.md, docs/04-API接口文档.md, docs/TASK_BOARD.md | T015-BE | 已明确 logout 后 accessToken 黑名单必须存储在 Redis，key 为 auth:blacklist:access:{tokenHash}，TTL 为 accessToken 剩余有效期，tokenHash 为 SHA-256 摘要；refreshToken 仍由 user_refresh_token.token_hash 保存 SHA-256 摘要 |
| T015-BE-REDIS | 复核并落地 Redis 黑名单实现 | 后端会话 | DONE | backend/, docs/TASK_BOARD.md | T015-AUTH-REDIS-DESIGN | 已将 accessToken 黑名单从本地 Map 替换为 Redis 必选存储，key 为 auth:blacklist:access:{tokenHash}，TTL 为 accessToken 剩余有效期；Redis 不可用时认证校验明确失败；已补充 logout 后原 accessToken 访问 /api/auth/me 被拒绝、Redis key 和 TTL 测试；已通过 mvn test（50 个测试）和 mvn -DskipTests package |
| T015-IT | 后端 MVP 主流程集成测试 | 测试会话 | DONE | backend/src/test/, backend/src/test/resources/, docs/05-测试用例.md, docs/TASK_BOARD.md | T015-BE-REDIS | 已新增 SpringBootTest + MockMvc 集成测试，连接 MySQL 8 测试库和 Redis 测试库，覆盖注册/登录/发起拼单/加入/详情/锁单/付款/取餐/我的拼单/看板/logout 黑名单及异常场景；发现的 /status 完成拼单接口契约差异已由 T017-API-CONTRACT 收口；已通过 mvn test（52 tests, 0 failures） |
| T017-API-CONTRACT | 对齐 API 文档契约 | 主控会话 | DONE | docs/04-API接口文档.md, docs/05-测试用例.md, docs/TASK_BOARD.md | T015-IT | 已明确 MVP 不使用 PATCH /api/group-orders/{id}/status 完成拼单，完成拼单统一通过 POST /api/group-orders/{id}/pickup-status 推进到 DISTRIBUTED 并同步 FINISHED；已补齐我的拼单 PENDING_PAYMENT/HISTORY scope 和看板 todayOrderCount/successOrderCount/totalSavedAmount；记录后端集成测试 52 tests, 0 failures |
| T016 | 创建前端工程骨架 | 前端会话 | DONE | frontend/, docs/TASK_BOARD.md | T005 | 已创建 Vue3 + Vite + TypeScript 前端工程骨架，集成 Element Plus、Vue Router、Pinia、Axios，完成基础布局、六个页面路由、mock 数据和 axios 封装；已通过 npm install、npm run build，并启动本地 Vite 服务验证首页与 /hall 路由返回 200 |
| T017 | 前端 API 封装与登录联调 | 前端会话 | DONE | frontend/, docs/TASK_BOARD.md | T016, T015-BE | 已封装 axios 实例、Authorization 自动携带、统一错误处理和 register/login/logout/refresh/me 认证 API；Pinia 保存 accessToken、refreshToken 与用户信息，退出登录调用后端 logout，刷新页面可通过 /api/auth/me 恢复登录态；拼单业务继续使用 mock；已通过 npm run build，并联调 register/login/me/refresh/logout 均返回 code=200 |
| T018 | 拼单大厅与发起拼单联调 | 前端会话 | DONE | frontend/, docs/TASK_BOARD.md | T017, T009 | 已接入 GET /api/group-orders 与 POST /api/group-orders，支持状态、类型、关键词和分页查询；大厅与发起拼单不再使用 mock，保留详情、加入、我的拼单和看板 mock；已通过 npm run build，并联调注册/登录/创建拼单/组合查询均返回 code=200 |
| T019 | 拼单详情与加入拼单联调 | 前端会话 | DONE | frontend/, docs/TASK_BOARD.md | T018, T010-BE | 已接入 GET /api/group-orders/{orderId} 与 POST /api/group-orders/{orderId}/participants；详情页展示基础信息、发起人、参与者、餐品、金额进度、满减差额、付款和取餐状态；加入成功后刷新详情；已删除详情和加入拼单 mock，保留锁单、付款、取餐、我的拼单和数据看板 mock；已通过 npm run build，并联调详情、加入、重复加入、金额非法、人数已满和已锁定错误提示 |
| T020 | 锁单、付款、取餐主流程联调 | 前端会话 | DONE | frontend/, docs/TASK_BOARD.md | T019, T011, T012-BE, T013 | 已在拼单详情页接入锁单、标记付款、确认付款、指定取餐人和更新取餐状态真实接口，并按发起人、参与者、取餐人身份控制按钮显示；已删除详情页锁单、付款、取餐模拟操作，保留我的拼单和数据看板 mock；已通过 npm run build，并联调创建/加入/非发起人锁单失败/锁单/标记付款/确认付款/重复付款失败/指定取餐人/非法取餐跳转/越权取餐更新/合法取餐推进到 FINISHED；注意当前后端取餐状态接口实际为 PATCH /api/group-orders/{id}/pickup-status，与 API 文档 POST 口径存在差异 |
| T021 | 我的拼单与数据看板联调 | 前端会话 | DONE | frontend/, docs/TASK_BOARD.md | T020, T014 | 已接入 GET /api/my/group-orders 与 GET /api/dashboard/summary；我的拼单支持我发起、我参与、待付款、历史拼单分页查询，数据看板展示今日拼单数、成功拼单数、累计节省金额、热门类型和热门店铺；已删除剩余我的拼单和数据看板 mock，frontend/src 下无 mock 残留；已通过 npm run build，并回归联调登录注册、拼单大厅、发起拼单、详情、加入、锁单、付款、取餐、我的拼单和数据看板均走真实接口 |
| V2-T001 | 全局 V2 设计重构 | 主控会话 | DONE | docs/01-需求说明.md, docs/02-领域模型与业务规则.md, docs/03-数据库设计.md, docs/04-API接口文档.md, docs/05-测试用例.md, docs/V2-升级设计说明.md, docs/TASK_BOARD.md | T021 | 已按 V2 重新梳理需求、领域、数据库建议、API 契约和测试矩阵；保留 MVP 能力，新增取消、超时、事件、并发、Redis/RocketMQ 边界和前端产品化方向；本任务未修改 backend/、frontend/、sql/schema.sql、sql/data.sql |
| V2-T002 | 根据 V2 设计修改数据库文档和 schema | SQL 会话 | TODO | docs/03-数据库设计.md, sql/schema.sql, sql/data.sql, docs/TASK_BOARD.md | V2-T001 | 根据 V2 设计落地 group_order 取消/过期/版本字段、group_order_event 表、必要索引和演示数据；同步数据库文档 |
| V2-T003 | 取消拼单接口 | 后端会话 | TODO | backend/, docs/TASK_BOARD.md | V2-T002 | 实现 POST /api/group-orders/{id}/cancel，补充权限校验、状态校验、取消原因、状态日志和事件记录 |
| V2-T004 | 超时关闭 + RocketMQ 延迟消息 | 后端会话 | TODO | backend/, docs/TASK_BOARD.md | V2-T002, V2-T003 | 实现 CREATED 超时进入 EXPIRED；RocketMQ 只触发超时检查，最终状态以 MySQL 事务和状态校验为准 |
| V2-T005 | 延迟/异常事件记录 | 后端会话 | TODO | backend/, docs/TASK_BOARD.md | V2-T002 | 实现 group_order_event 新增与查询接口；延迟、缺餐、联系失败、付款争议等记录为事件，不扩散主状态 |
| V2-T006 | Redis 防重复提交和锁单短时锁 | 后端会话 | TODO | backend/, docs/TASK_BOARD.md | V2-T003, V2-T004, V2-T005 | 为关键写接口补充防重复提交；锁单等高冲突操作可使用 Redis 短时锁辅助，但正确性仍以 MySQL 为准 |
| V2-T007 | 取消/超时/延迟/并发测试 | 测试会话 | TODO | backend/src/test/, docs/05-测试用例.md, docs/TASK_BOARD.md | V2-T003, V2-T004, V2-T005, V2-T006 | 覆盖取消权限、EXPIRED 超时关闭、延迟/异常事件、重复提交、锁单并发和 MQ 重复消费等场景 |
| V2-T008 | 拼单大厅产品化改版 | 前端会话 | TODO | frontend/, docs/TASK_BOARD.md | V2-T001 | 将大厅从后台表格改为校园拼单产品首页，突出拼单卡片、取餐点、倒计时、凑单进度和可加入状态 |
| V2-T009 | 详情页流程式改版 | 前端会话 | TODO | frontend/, docs/TASK_BOARD.md | V2-T003, V2-T005, V2-T008 | 按拼单协同流程重构详情页，突出主状态、成员金额、付款、取餐、事件时间线和当前用户可操作项 |
| V2-T010 | 异常状态、取消、延迟提示联调 | 前端会话 | TODO | frontend/, docs/TASK_BOARD.md | V2-T004, V2-T005, V2-T009 | 联调取消、EXPIRED、延迟/异常事件提示和终态操作限制，确保前端口径与 V2 API 契约一致 |
## 使用规则

- 开始任务前将状态改为 `DOING`。
- 完成并验证后将状态改为 `DONE`。
- 无法继续时将状态改为 `BLOCKED`，并在备注中说明原因。
- 不删除历史任务；废弃任务在备注中说明。
