# LLM 辅助开发记录

本文档用于记录“校园拼好饭系统”开发过程中由 LLM 辅助完成的需求梳理、设计建议、代码生成、问题排查和验证情况。

## 记录原则

- 记录 LLM 参与的重要修改，不记录无意义的临时问答。
- 每次记录应说明修改目标、涉及文件、主要内容和验证方式。
- 对 LLM 生成的代码或方案必须由开发者审查后再采用。
- 涉及金额计算、状态流转、权限校验和数据一致性的修改，应重点记录验证情况。

## 记录模板

```text
日期：
参与方式：
目标：
涉及文件：
主要改动：
验证方式：
结果：
后续事项：
```

## 记录

### 2026-05-26

日期：2026-05-26

参与方式：LLM 根据项目初始化要求创建基础文档。

目标：建立仓库根目录文档、开发计划文档和 SQL 目录说明，明确项目定位、技术栈和开发约束。

涉及文件：

- `AGENTS.md`
- `README.md`
- `docs/LLM辅助开发记录.md`
- `docs/开发计划.md`
- `sql/README.md`

主要改动：

- 明确校园拼好饭系统的项目定位。
- 明确后端 Spring Boot 3、Java 21、MyBatis 3、MySQL 8 技术栈。
- 明确前端 Vue3、Vite、TypeScript、Element Plus、Pinia、Axios 技术栈。
- 约定金额计算必须使用 `BigDecimal`。
- 约定状态流转必须由后端校验。
- 约定不要主动引入未要求的复杂中间件。

验证方式：

- 检查指定文档是否创建完成。
- 检查文档内容是否覆盖初始化要求。

结果：已完成初始化文档创建。

后续事项：

- 创建后端工程骨架。
- 创建前端工程骨架。
- 设计数据库表结构和初始化 SQL。
- 明确接口规范和状态枚举。

### 2026-05-29 主控会话阶段总结

日期：2026-05-29

参与方式：主控会话使用 LLM 持续辅助需求拆解、领域建模、接口与数据库契约收口、多会话协作规则设计，以及 MVP 到 V2 的全局设计重构。

目标：记录主控会话在校园拼好饭系统从项目选题、MVP 设计、契约对齐、认证升级到 V2 设计重构过程中的实际作用、人工判断点和对后续开发的影响。

涉及文件：

- `AGENTS.md`
- `docs/TASK_BOARD.md`
- `docs/01-需求说明.md`
- `docs/02-领域模型与业务规则.md`
- `docs/03-数据库设计.md`
- `docs/04-API接口文档.md`
- `docs/05-测试用例.md`
- `docs/CONTRACT_ALIGNMENT_REPORT.md`
- `docs/V2-升级设计说明.md`

主要改动：

1. 项目选题与需求分析

   LLM 辅助将项目定位从泛泛的“点餐/外卖系统”收束为“面向高校学生的拼单、凑单、金额分摊、付款标记与取餐协同系统”。主控会话明确系统不是外卖平台，不接入真实支付、不接入真实商家接口、不做即时聊天，核心价值集中在校园拼单协同。

   在需求文档中，LLM 辅助拆分了项目背景、用户角色、核心业务流程、功能模块、MVP 范围、增强范围和暂不实现范围。人工判断主要体现在：拒绝把系统扩大成外卖交易平台，避免把商家、支付、配送、聊天、复杂运营后台提前纳入 MVP。

2. 领域模型和状态流转设计

   LLM 辅助梳理了 `User`、`GroupOrder`、`OrderParticipant`、`MealItem`、`PaymentRecord`、`PickupRecord`、`NotificationRecord` 等核心对象，并明确 `group_order` 与 `order_participant` 的一对多关系：一个拼单包含多个成员参与记录，同一用户在同一拼单中只能有一条有效参与记录。

   状态设计中，主控会话多次控制状态数量，避免“状态爆炸”。MVP 阶段曾将 `EXPIRED` 延后，规定超时或不继续拼单由发起人手动取消，状态使用 `CANCELLED`。V2 重构时，在保留 MVP 能力基础上重新引入 `EXPIRED`，但明确其只表示系统超时关闭，和发起人主动取消的 `CANCELLED` 严格区分。延迟、缺餐、联系失败、付款争议等不进入主状态，统一记录为履约事件。

   金额规则方面，LLM 辅助形成了优惠按成员原始金额比例分摊、尾差由最后一名成员承担、金额计算必须使用 `BigDecimal`、锁单后固化分摊结果等规则。人工判断主要集中在优惠分摊策略是否足够简单、可解释、便于后端实现和测试。

3. API、数据库、认证方案冲突收口

   主控会话多次处理前后端、SQL、测试之间的契约不一致，LLM 辅助生成对齐报告和文档修订建议。

   已出现并修正的主要不一致：

   - 拼单 API 路径不一致：任务中曾出现 `/api/orders`，最终统一为 `/api/group-orders`，并明确 `docs/04-API接口文档.md` 是前后端接口唯一契约。
   - 用户账号字段不一致：数据库使用 `user.account`，API 对外统一使用 `username`，避免前端和后端 VO 混用 `account`。
   - 发起拼单缺少取餐地点：补充 `group_order.pickup_location` 表示约定取餐地点，同时保留 `pickup_record.pickup_location` 表示实际取餐地点或履约阶段地点。
   - 认证方案逐步升级：先收口为 MVP 轻量真实登录，支持 register/login/me；后续升级为 accessToken + refreshToken，并新增 refresh/logout。人工判断明确不引入完整 Spring Security、OAuth2、短信验证码、复杂 RBAC。
   - token 存储口径不一致：最终明确 token 不明文存储，refreshToken 只保存 SHA-256 摘要；logout 后 accessToken 黑名单必须存储在 Redis，key 为 `auth:blacklist:access:{tokenHash}`，TTL 为 accessToken 剩余有效期。
   - 完成拼单接口口径不一致：MVP 不再使用 `PATCH /api/group-orders/{id}/status` 作为完成拼单接口，统一通过 `POST /api/group-orders/{id}/pickup-status` 推进取餐状态到 `DISTRIBUTED`，并同步拼单主状态为 `FINISHED`。
   - 我的拼单和看板字段不完整：补齐 `PENDING_PAYMENT`、`HISTORY` scope，以及 `todayOrderCount`、`successOrderCount`、`totalSavedAmount` 等看板字段。

   这些冲突通过主控会话先改文档契约，再交回后端、前端、SQL 或测试会话继续实现，避免各会话直接按自己的理解写代码。

4. 多会话协作规则设计

   LLM 辅助将根目录 `AGENTS.md` 从堆叠式规则重构为索引型全局规则，并把后端、前端、SQL、文档、多会话协作细则拆到对应目录或 `docs/CODEX_WORKFLOW.md`。

   主控会话明确了角色边界：

   - 主控会话负责需求、任务拆分、接口约定、数据库设计和协作协调；
   - 后端会话负责 `backend/`；
   - 前端会话负责 `frontend/`；
   - SQL 会话负责 `sql/` 和数据库设计同步；
   - 测试会话负责测试用例、接口验证和 Bug 复现；
   - 文档汇报会话负责演示材料和 LLM 记录。

   人工判断点主要是文件修改边界和任务看板规则：接口和数据库变更必须先更新文档，发现冲突时暂停实现并报告，不允许某个会话直接跨目录修改或猜测合并。

5. 从 MVP 到 V2 前的全局把控

   MVP 阶段，主控会话重点保证主流程可闭环：登录、发起拼单、成员加入、凑单计算、锁定拼单、优惠分摊、付款标记、指定取餐人、更新取餐状态、完成拼单。

   当项目暴露出“功能偏简陋、文档补丁化、前端偏管理后台”的问题后，LLM 辅助主控会话进行了 V2 全局设计重构。V2 重新梳理了需求、领域、数据库建议、API 契约和测试矩阵，新增取消拼单、超时自动关闭、延迟/异常事件记录、并发控制、RocketMQ 异步消息、Redis 防重复提交/短时锁/热点缓存和前端产品化改版。

   人工判断主要体现在 V2 范围控制：取消、超时、事件、并发和前端产品化列为 V2 必做；Redis 热点缓存、RocketMQ 通知和统计聚合等作为增强能力，不允许成为核心状态源。并发控制明确以 MySQL 事务和状态校验为主，Redis 只能辅助。

验证方式：

- 通过阅读任务看板确认主控会话已完成 T001、T002、T010、BUG-QA-002、T015-AUTH-DESIGN、T015-AUTH-REDIS-DESIGN、T017-API-CONTRACT、V2-T001 等关键协调任务。
- 通过 `docs/CONTRACT_ALIGNMENT_REPORT.md` 复核 API 路径、用户字段、取餐地点字段和中间件边界的统一决策。
- 通过 V2 需求和领域文档复核 `CANCELLED`、`EXPIRED`、付款状态、取餐状态、履约事件和并发控制口径。

结果：

主控会话通过 LLM 辅助完成了从需求到契约再到任务拆分的持续收口，使后端、前端、SQL、测试和文档会话能够围绕同一组文档继续开发。项目从最初的 MVP 拼单协同系统，演进为有明确 V2 目标、状态模型、接口边界、数据库变更建议和测试矩阵的校园拼单产品。

后续事项：

- SQL 会话执行 `V2-T002`，根据 V2 设计修改数据库文档和 `schema.sql`。
- 后端会话依次实现取消拼单、超时关闭、履约事件和 Redis 并发辅助能力。
- 测试会话补齐取消、超时、延迟和并发测试。
- 前端会话按 V2 设计重做拼单大厅和详情页，避免继续沿用管理后台式体验。
- 文档汇报会话持续更新本文件，记录后续 LLM 生成内容、人工调整和验证结果。

### 2026-05-29 前端会话阶段总结

日期：2026-05-29

参与方式：前端会话使用 LLM 辅助创建 Vue3 前端工程、搭建页面、封装 API、管理登录态和拼单状态，并按任务看板逐步把 mock 数据迁移为真实后端接口。

目标：记录前端会话从 T016 到 T021 的实际作用、LLM 生成内容、人工把控点、联调问题和 V2 前端优化方向。

涉及文件：

- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/src/main.ts`
- `frontend/src/router/index.ts`
- `frontend/src/api/http.ts`
- `frontend/src/api/auth.ts`
- `frontend/src/api/order.ts`
- `frontend/src/api/token.ts`
- `frontend/src/stores/user.ts`
- `frontend/src/stores/orders.ts`
- `frontend/src/types/auth.ts`
- `frontend/src/types/order.ts`
- `frontend/src/layouts/MainLayout.vue`
- `frontend/src/views/LoginView.vue`
- `frontend/src/views/HallView.vue`
- `frontend/src/views/CreateOrderView.vue`
- `frontend/src/views/OrderDetailView.vue`
- `frontend/src/views/MyOrdersView.vue`
- `frontend/src/views/DashboardView.vue`
- `frontend/src/components/AmountStat.vue`
- `frontend/src/components/StatusTag.vue`
- `frontend/src/components/OrderCard.vue`
- `frontend/src/styles.css`
- `docs/TASK_BOARD.md`

使用阶段：

1. 创建 Vue3 前端工程骨架

   LLM 辅助创建了 Vue3 + Vite + TypeScript 前端工程，并集成 Element Plus、Vue Router、Pinia 和 Axios。工程结构按 `api`、`router`、`stores`、`types`、`utils`、`views`、`components`、`layouts` 拆分，使后续联调任务可以在统一目录边界内推进。

   LLM 主要帮助生成了基础路由、主布局、顶部导航、侧边栏、页面容器、通用金额组件和状态标签组件。人工把控点主要是确认技术栈与 `frontend/AGENTS.md` 一致，避免引入未要求的 UI 框架或额外状态管理方案。

2. 搭建静态页面和 mock 数据

   在后端接口尚未全部接入前，LLM 辅助搭建了登录页、拼单大厅页、发起拼单页、拼单详情页、我的拼单页和数据看板页，并根据 API 文档构造 mock 数据。mock 阶段的目标是让课堂演示的主流程页面先能跑通，方便评估信息结构和交互位置。

   这一阶段 LLM 输出了卡片、表格、金额概览、参与者列表、取餐协同、数据看板等页面骨架。但页面整体更接近管理后台，偏表单和表格，缺少校园拼单产品应有的场景感、紧迫感和社交协同感，这一点需要人工产品判断，不能只依赖模型默认生成的后台风格。

3. 完成登录注册联调

   LLM 辅助封装了 Axios 实例，统一设置 `baseURL: /api`，在请求拦截器中自动携带 `Authorization: Bearer accessToken`，在响应拦截器中统一处理业务错误和 HTTP 错误。认证 API 拆分为 `register`、`login`、`logout`、`refresh`、`me`，登录成功后通过 Pinia 和本地存储保存 `accessToken`、`refreshToken` 和用户信息。

   用户状态管理由 `stores/user.ts` 承担，支持刷新页面后通过 `/api/auth/me` 恢复当前用户；退出登录时调用后端 `logout` 并清理本地 token。人工把控点是确认 token 不作为业务权限依据，前端只做体验层状态恢复，真正权限仍由后端校验。

4. 逐步接入拼单业务接口

   LLM 辅助按任务顺序将业务模块从 mock 迁移到真实接口：

   - T018：接入拼单大厅 `GET /api/group-orders` 和发起拼单 `POST /api/group-orders`，支持状态、类型、关键词和分页查询。
   - T019：接入拼单详情 `GET /api/group-orders/{id}` 和加入拼单 `POST /api/group-orders/{id}/participants`，详情页展示发起人、参与者、餐品明细、金额进度、满减差额、付款状态和取餐状态。
   - T020：接入锁单、付款和取餐主流程，包括锁单、参与者标记付款、发起人确认付款、指定取餐人和更新取餐状态，并按当前用户身份控制按钮显示。
   - T021：接入我的拼单 `GET /api/my/group-orders` 和数据看板 `GET /api/dashboard/summary`，展示我发起、我参与、待付款、历史拼单，以及今日拼单数、成功拼单数、累计节省金额、热门类型和热门店铺。

   状态管理方面，LLM 辅助扩展了 `stores/orders.ts`，统一维护拼单列表、详情、我的拼单、数据看板、loading、分页和筛选条件。页面层只调用 store action，不直接拼接复杂请求逻辑。

5. 每个模块联调成功后删除对应 mock

   前端会话在 `frontend/AGENTS.md` 中补充了规则：某模块完成真实接口联调后，必须删除该模块对应 mock 数据和 mock API，后续不得继续通过 mock 兜底该模块主流程。

   实际迁移过程为：

   - 认证联调后删除登录、注册、当前用户、退出相关 mock。
   - 大厅和发起拼单联调后删除列表与创建相关 mock。
   - 详情和加入拼单联调后删除详情参与者、餐品和取餐记录 mock。
   - 锁单、付款、取餐联调后删除详情页模拟操作按钮。
   - 我的拼单和数据看板联调后删除 `frontend/src/api/mock.ts`，并确认 `frontend/src` 下无 `mock`、`Mock`、`fetch*Mock` 残留。

Prompt 摘要：

- “创建前端工程骨架，使用 Vue3 + Vite + TypeScript，集成 Element Plus、Router、Pinia、Axios。”
- “实现认证模块 API 封装与登录联调，保存 token 和用户信息。”
- “清理认证模块 mock，并记录联调成功后删除对应 mock 的前端规则。”
- “实现拼单大厅、发起拼单、详情、加入拼单、锁单、付款、取餐、我的拼单、数据看板真实接口联调。”
- “修复详情页取餐状态显示问题和付款按钮显示问题。”

模型输出内容：

- 生成前端工程目录、基础布局、路由和页面组件。
- 生成 Axios 封装、API 模块、类型定义、Pinia store 和 token 存储逻辑。
- 根据 API 文档补齐请求类型、响应类型和页面字段展示。
- 根据当前用户身份生成按钮显示逻辑，例如发起人可锁单、确认付款、指定取餐人，参与者可标记自己付款，取餐人可更新取餐状态。
- 生成 loading、空状态、错误提示、分页和筛选等常规前端交互。
- 根据联调结果删除对应 mock，并更新任务看板。

联调中出现的问题：

1. 接口方法口径差异

   API 文档中取餐状态接口为 `POST /api/group-orders/{id}/pickup-status`，但当前后端实际实现为 `PATCH /api/group-orders/{id}/pickup-status`。前端为了完成真实接口联调按后端实际实现接入 `PATCH`，并在 T020 任务看板备注中记录该差异。该问题需要主控或后端会话后续统一文档和实现。

2. 创建拼单参数与后端校验差异

   联调中曾将 `discountThresholdAmount` 传为 `0`，后端校验要求该字段要么不传，要么大于 `0`。前端需要避免把“无满减”表达为非法数值，相关页面和类型需继续保持和后端校验一致。

3. 付款按钮显示误解

   详情页中参与者在拼单 `CREATED` 状态下看到“无可用操作”，容易误以为付款按钮缺失。后端规则是锁单生成应付金额后才允许标记付款，因此前端改为在未锁单时显示禁用的“待锁单”按钮，并提示“锁单生成应付金额后才能标记付款”。

4. 取餐状态显示问题

   Element Plus 纵向步骤条在未指定取餐人时仍渲染，导致页面出现过长竖线和空白。前端改为未指定取餐人时显示占位提示，只有存在取餐记录后才显示紧凑步骤条。

5. 构建和运行环境问题

   多次 `npm run build` 可以通过，但 Vite 构建存在 Element Plus 相关 chunk 超过 500KB 的 warning。该问题不影响 MVP 演示，但 V2 可考虑路由级懒加载、手动分包或按需优化。

验证方式：

- 多次执行 `npm run build`，确认 `vue-tsc -b && vite build` 通过。
- 通过接口级脚本联调 register/login/me/refresh/logout。
- 通过接口级脚本联调拼单大厅、创建拼单、详情、加入、重复加入、人数已满、金额非法、锁单、付款、指定取餐人、取餐状态推进、我的拼单和数据看板。
- 通过 `rg "mock|Mock|fetch.*Mock" frontend/src` 确认 T021 后前端主流程 mock 已清空。
- 通过任务看板确认 T016 到 T021 已完成并记录验证结果。

结果：

前端会话在 LLM 辅助下完成了从工程骨架、静态页面、mock 演示到真实接口闭环的迁移。当前 MVP 前端已经覆盖登录注册、拼单大厅、发起拼单、拼单详情、加入拼单、锁单、付款、取餐、我的拼单和数据看板，且主流程不再依赖 mock。

需要人工把控的 UI 和产品体验：

- 当前页面整体偏管理后台，信息组织以表格、表单、数据面板为主，适合开发联调和课堂说明，但不像面向学生的拼单产品。
- 拼单大厅缺少产品化的第一眼吸引力，应突出“可加入拼单”“离截止还有多久”“还差多少钱满减”“取餐点在哪里”“已有谁参与”等用户决策信息。
- 拼单详情页虽然覆盖主流程字段，但信息密度较高，操作区偏后台表单，缺少围绕当前用户任务的流程引导。
- 支付和取餐操作需要更强的状态解释，例如为什么现在不能付款、谁可以确认付款、为什么需要先指定取餐人。
- 模型可以快速生成布局和交互，但很容易默认采用后台模板。是否符合校园学生使用场景、是否有产品感，需要人工继续审查和重构。

模型局限：

- LLM 能根据文档快速生成 API 封装和页面结构，但不能自动保证接口文档与后端实现完全一致，仍需通过真实联调发现 `POST/PATCH` 等口径差异。
- LLM 默认页面风格偏通用后台，缺少针对校园拼单场景的产品表达。
- LLM 可以生成按钮权限逻辑，但业务语义是否易懂需要人工从用户角度检查，例如“付款按钮为什么不显示”这类体验问题。
- LLM 生成的 mock 容易残留，必须通过明确规则和代码检索来强制删除。

后续 V2 前端优化方向：

- 执行 `V2-T008`，将拼单大厅从后台列表改为校园拼单产品首页，使用更强的拼单卡片、取餐点、倒计时、凑单进度、可加入状态和快捷筛选。
- 执行 `V2-T009`，将详情页改为流程式协同页面，围绕“当前状态、我该做什么、其他人进度、金额分摊、付款、取餐、事件时间线”组织信息。
- 联调取消、超时关闭、延迟/异常事件后，在前端提供清晰的终态、异常提示和操作限制。
- 增加移动端适配，优先保证学生在手机上查看拼单、加入拼单、标记付款和查看取餐状态的体验。
- 优化性能和打包体积，考虑路由懒加载、Element Plus 按需优化和图标按需引入。
- 加强错误状态设计，不只弹出错误消息，还要在页面内解释原因和下一步可操作项。

后续事项：

- 主控或后端会话统一 `pickup-status` 接口文档与后端实现的 HTTP 方法差异。
- 前端会话继续按 V2 任务优化大厅和详情页产品体验。
- 文档汇报会话可基于本节提炼 PPT 中的“LLM 在前端开发中的作用与局限”。

### 2026-05-29 后端会话阶段总结

日期：2026-05-29

参与方式：后端会话使用 LLM 辅助阅读全局规则、后端规则、API 文档、领域规则、数据库脚本和任务看板，在限定修改范围内分阶段生成 Spring Boot 代码、修复编译问题、补充测试并同步任务状态。

目标：总结 LLM 在后端工程从骨架创建到 MVP 主流程闭环、认证升级、Redis token 黑名单和测试反馈修复过程中的作用、效率提升、阻塞点和人工收口点。

涉及文件：

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/campus/pinhaofan/**`
- `backend/src/test/java/com/campus/pinhaofan/**`
- `backend/src/test/resources/application-test.yml`
- `docs/TASK_BOARD.md`

主要改动：

1. 创建 Spring Boot 后端工程骨架

   LLM 辅助快速创建了 Java 21 + Spring Boot 3.2.x + Maven 后端工程，集成 MyBatis-Plus、MySQL、Lombok、Validation，并补齐统一返回 `Result`、全局异常处理、CORS 配置和本地 MySQL `campus_haofan` 连接配置。

   在这一阶段，LLM 的主要价值是减少重复工程配置成本：`pom.xml`、包结构、启动类、基础配置、异常处理和响应格式可以一次性成型，使后续任务直接进入业务实现。人工审查重点是确认依赖版本、包名、配置项和后端目录边界符合 `AGENTS.md` 与 `backend/AGENTS.md`。

2. 生成 entity、Mapper 和状态枚举

   LLM 根据 `sql/schema.sql` 生成了用户、刷新 token、拼单、参与者、餐品、付款记录、取餐记录、状态日志和通知记录等实体类，并为每张表生成 MyBatis-Plus `BaseMapper`。字段从数据库 `snake_case` 映射为 Java 驼峰命名，金额字段使用 `BigDecimal`，时间字段使用 `LocalDateTime`，主键使用 `Long`。

   同时生成 `GroupOrderStatus`、`PaymentStatus`、`PickupStatus`、`NotificationStatus` 等枚举，为后续状态校验提供集中口径。LLM 提升了结构性代码生成效率，但仍需要人工确认数据库字段是否已被契约收口，例如 `user.account` 与 API `username` 的映射、`group_order.pickup_location` 的补齐，以及 `user_refresh_token` 表的认证升级字段。

3. 实现用户认证、轻量 token、refreshToken 和 Redis 黑名单

   LLM 辅助实现了注册、登录、获取当前用户、刷新 token 和登出接口。认证实现遵守 MVP 约束，不引入完整 Spring Security、OAuth2、短信验证码或复杂 RBAC。注册时校验账号唯一，密码使用 BCrypt 写入 `password_hash`；登录返回 `accessToken`、`refreshToken`、`expiresIn` 和用户信息；`refreshToken` 只保存 SHA-256 摘要到 `user_refresh_token.token_hash`，刷新时轮换并作废旧记录；登录时更新 `last_login_time` 和 `last_login_ip`。

   认证升级过程中出现过一次重要人工收口：早期 accessToken 黑名单先以本地 Map 实现，后来主控契约明确必须使用 Redis。后端会话在 LLM 辅助下将黑名单替换为 `StringRedisTemplate` 存储，key 固定为 `auth:blacklist:access:{tokenHash}`，`tokenHash` 为 accessToken 的 SHA-256 摘要，TTL 为 accessToken 剩余有效期。token 校验时必须查 Redis，命中即拒绝；Redis 不可用时认证接口明确失败，不再降级到本地 Map。

   这一阶段 LLM 对认证流程串联、token 摘要存储和测试覆盖有明显帮助，但安全边界仍需要人工把关，包括不保存明文 token、不误引入重型鉴权框架、Redis 不可用时是否允许降级等决策。

4. 实现拼单大厅、发起拼单、详情和加入拼单

   LLM 辅助实现了 `GET /api/group-orders`、`POST /api/group-orders`、`GET /api/group-orders/{orderId}` 和 `POST /api/group-orders/{orderId}/participants`。大厅查询支持状态、类型、关键词和分页筛选；创建拼单时使用当前登录用户作为 `creator_id`，默认状态为 `CREATED`，并写入 `order_status_log`；详情接口返回拼单基础信息、发起人、参与者、餐品明细、当前金额、满减差额、付款状态和取餐状态；加入拼单校验 `CREATED` 状态、截止时间、重复加入、人数上限和餐品金额，并更新订单金额汇总。

   这一阶段暴露过 API 路径不一致阻塞：早期任务中出现 `/api/orders`，而全局契约最终收口为 `/api/group-orders`。LLM 能快速批量改造 Controller、Service、DTO、VO 和测试，但路径和字段口径必须由人工依据 API 文档统一确认。

5. 实现锁单、金额分摊、付款确认、取餐状态、我的拼单和数据看板

   锁单与优惠分摊是后端复杂度最高的部分之一。LLM 辅助实现了发起人权限校验、`CREATED -> LOCKED` 状态校验、基于 `meal_item.subtotal_amount` 重新汇总金额、按参与者原始金额占比分摊优惠、金额保留两位小数、尾差由发起人承担或补齐，并更新 `group_order` 与 `order_participant` 的金额字段。实现中曾出现半完成代码和编译错误，后端会话通过检查当前代码而不是直接重写，补齐 VO、Service 方法和金额计算逻辑后完成 T011。

   付款模块中，LLM 辅助实现成员标记自己已付款、发起人确认任意成员付款、重复付款和重复确认的明确错误处理，并写入 `payment_record`。取餐模块中，LLM 辅助实现指定取餐人、发起人或取餐人按顺序推进取餐状态、同步推进拼单主状态、更新 `pickup_record` 并写入状态日志。我的拼单与数据看板阶段，LLM 辅助实现基于当前登录用户的发起、参与、待付款、历史拼单查询，以及今日拼单数、成功拼单数、累计节省金额、热门类型和店铺等演示统计。

   在这些业务阶段，LLM 对金额计算、状态流转和权限校验的帮助主要体现在快速列出校验路径、补齐异常分支、生成 Service 层单元测试和保持 DTO/VO 结构完整。但关键规则仍需要人工重新收口：状态能否跨越、终态是否允许修改、发起人是否必须作为参与者承担尾差、取餐人是否必须是参与者、付款状态是否只是线下协同标记等。

6. 根据测试会话反馈修复问题

   测试会话反馈推动了多处后端修复。典型问题包括：

   - 拼单大厅默认排序问题：未指定状态时，后端调整为未结束拼单优先，同一优先级内按 `create_time desc`。
   - `EXPIRED` 状态口径差异：MVP 阶段后端不处理 `EXPIRED`，由主控会话收口为超时不继续拼单时手动取消为 `CANCELLED`；V2 再重新设计 `EXPIRED`。
   - 完成拼单接口契约差异：后端和文档曾在 `PATCH /api/group-orders/{id}/status` 与取餐状态接口之间存在不一致，后续由主控收口为通过取餐状态推进到 `DISTRIBUTED` 并同步 `FINISHED`。
   - 认证黑名单存储口径差异：本地 Map 实现被替换为 Redis 必选存储，并补充 logout 后原 accessToken 访问 `/api/auth/me` 被拒绝的测试。

   LLM 在问题修复中提升了定位和修改效率，尤其是能根据测试报错快速回溯 Controller、Service、Mapper、DTO/VO 和任务看板。但测试反馈中的契约类问题不能只靠代码局部修补，需要人工先判断应以哪份文档为准，再决定是改后端、改测试还是改 API 文档。

验证方式：

- 后端会话多次执行 `mvn test` 和 `mvn -DskipTests package`，在各阶段确认编译和单元测试通过。
- 认证 Redis 黑名单阶段通过 50 个测试，覆盖 logout 写 Redis key、TTL、SHA-256 摘要和原 accessToken 访问 `/api/auth/me` 被拒绝。
- MVP 集成测试阶段覆盖注册、登录、发起拼单、加入、详情、锁单、付款、取餐、我的拼单、看板和 logout 黑名单等主流程。
- 通过 `docs/TASK_BOARD.md` 复核 T006、T007、T008、T009、T010-BE、T011、T012-BE、BUG-QA-001、T013、T014、T015-BE、T015-BE-REDIS、T015-IT 等后端相关任务状态。

结果：

后端会话在 LLM 辅助下完成了从空工程到 MVP 主流程闭环的主要实现。LLM 显著提升了工程骨架、实体 Mapper、DTO/VO、Service 校验逻辑和测试用例的生成效率；在金额计算、状态流转和权限校验上，LLM 能帮助系统性列出分支并减少遗漏。代码质量方面，统一响应、全局异常、状态枚举、事务边界、金额 `BigDecimal` 计算和单元测试覆盖都有明显收益。

同时，后端开发仍依赖人工进行关键收口。LLM 容易在上下文变更后沿用旧路径、旧字段或旧状态口径，例如 `/api/orders` 与 `/api/group-orders`、`account` 与 `username`、缺失 `pickupLocation`、本地 Map 黑名单与 Redis 黑名单、`EXPIRED` 是否进入 MVP、完成拼单接口路径等。最终可用性来自“LLM 快速生成 + 人工契约审查 + 测试会话回归”的组合，而不是直接采纳一次性生成结果。

后续事项：

- V2 后端实现前，应先等待 SQL 会话完成 V2 schema 和数据库文档同步。
- 取消拼单、超时关闭、履约事件和并发保护必须继续坚持 MySQL 为最终状态源，Redis/RocketMQ 只做辅助。
- 后续 LLM 生成业务代码时，应先读取最新 API 文档和领域规则，避免沿用 MVP 旧接口。
- 测试会话应继续覆盖状态冲突、并发加入、重复锁单、重复 MQ 消费和 Redis 不可用等场景。

### 2026-05-29 测试会话阶段总结

日期：2026-05-29

参与方式：测试会话使用 LLM 辅助阅读接口文档、领域规则、任务看板和后端代码，生成 Service 单元测试、MockMvc 集成测试、测试报告和缺陷记录，并把测试发现反馈给主控会话和后端会话。

目标：总结测试会话在 T008-T015 阶段对后端 MVP 质量保障、契约审查和主流程验证的作用。

涉及文件：

- `backend/src/test/java/com/campus/pinhaofan/service/AuthServiceImplTest.java`
- `backend/src/test/java/com/campus/pinhaofan/service/GroupOrderServiceImplTest.java`
- `backend/src/test/java/com/campus/pinhaofan/integration/MvpFlowIntegrationTest.java`
- `backend/src/test/resources/application-test.yml`
- `docs/05-测试用例.md`
- `docs/TASK_BOARD.md`

主要改动：

1. 为 T008-T012 补充 Service 测试

   LLM 辅助测试会话从 `docs/04-API接口文档.md`、`docs/02-领域模型与业务规则.md` 和当前 Service 实现中提炼测试矩阵，将用户认证、拼单大厅、发起拼单、拼单详情、加入拼单、锁单分摊、付款标记和付款确认拆成可执行的单元测试。

   测试重点包括：

   - 注册成功、重复注册、密码错误登录、无 token 访问 `/me`。
   - 创建拼单后默认 `CREATED`，并写入状态日志。
   - `CREATED` 状态允许加入，同一用户不能重复加入。
   - `LOCKED` 后禁止继续加入。
   - 锁单只允许发起人执行。
   - 满减达到时按金额比例分摊，未达到满减时优惠为 0。
   - 小数尾差写入 `rounding_adjustment_amount`。
   - 参与者只能标记自己的付款，发起人才能确认付款。
   - 重复付款、重复确认和非法订单状态不能产生脏数据。

   LLM 的价值在于快速把文档规则转成断言清单，并结合 Mockito 捕获 Mapper 入参，验证 Service 是否真的更新了状态、金额和日志字段。人工判断主要体现在筛选核心用例：不是把所有 DTO 字段都机械测一遍，而是优先覆盖权限、状态流转、金额一致性和重复提交这些高风险路径。

2. 发现并推动修正真实问题

   测试会话不仅补测试，也做契约审查。LLM 辅助对比 API 文档、领域规则和后端实现后，记录了多个真实问题：

   - `BUG-QA-001`：拼单大厅默认排序与 API 契约不一致。文档要求未结束拼单优先，同类按 `create_time desc`，后端最初只按创建时间倒序。测试会话补充排序断言后，推动后端修复默认排序。
   - `BUG-QA-002`：`EXPIRED` 状态口径不一致。测试会话发现付款非法状态测试和文档中出现 `EXPIRED`，但 MVP 阶段不实现该状态。该问题推动主控会话收口为“EXPIRED 延后，MVP 超时不继续拼单由发起人手动取消为 CANCELLED”。
   - 付款接口缺口：早期测试审查中曾发现 API 文档要求付款标记和确认，但后端尚未实现对应 Controller/Service。测试报告推动后端会话补齐付款模块。
   - `BUG-QA-003`、`BUG-QA-004`：我的拼单 scope 和数据看板响应字段文档不完整，推动后续 API 契约补齐。
   - `BUG-QA-005`：完成拼单接口口径不一致。API 文档曾保留 `/status`，但后端实际通过取餐状态推进到 `DISTRIBUTED` 同步 `FINISHED`。该问题推动主控会话用 `T017-API-CONTRACT` 收口接口契约。

   这些问题说明测试会话的价值不只是“补覆盖率”，还包括用可复现的测试和文档化问题推动其他会话修正实现或契约。

3. 覆盖付款、锁单、金额分摊、重复加入和非法状态边界

   LLM 根据业务规则生成了边界测试候选，测试会话再按风险排序落地。最终重点覆盖：

   - 金额分摊：达到满减、未达满减、三人均摊导致小数尾差、成员应付汇总等于订单应付总额。
   - 锁单：非发起人锁单拒绝、非 `CREATED` 状态锁单拒绝、发起人未作为参与者加入时不能承接尾差。
   - 加入：重复加入、锁单后加入、截止时间和人数上限相关规则。
   - 付款：非本人标记付款、非发起人确认、未标记直接确认、重复标记、重复确认、非法订单状态付款拒绝。
   - 取餐：指定取餐人权限、取餐人必须属于拼单、普通参与者不能更新状态、状态不能跳跃、`FINISHED` 后不可修改。

   对金额和状态测试，LLM 可以快速生成正常路径和异常路径，但人工仍需确认预期值。例如优惠分摊尾差到底由谁承担、`LOCKED -> ORDERED` 是指定取餐人时同步推进还是另设接口推进，都必须以主控和后端最终口径为准。

4. 做后端 MVP 集成测试

   在 Service 测试稳定后，测试会话进一步补充 `MvpFlowIntegrationTest`，使用 `SpringBootTest + MockMvc` 覆盖 Controller 到真实 MySQL/Redis 的完整链路。测试 profile 使用 `application-test.yml`，连接 `campus_haofan_test` 和 Redis 测试库；每个集成测试前重建 schema 并清空 Redis 测试库。

   MVP 主流程集成测试覆盖：

   - 注册用户 A、B、C。
   - 用户 A 登录，获得 `accessToken` 和 `refreshToken`。
   - 用户 A 发起拼单。
   - 查询拼单大厅，确认新拼单存在。
   - 用户 A、B、C 加入拼单。
   - 查询拼单详情，确认参与者和金额进度。
   - 用户 A 锁单，确认 `LOCKED`、满减和应付金额。
   - 用户 B、C 标记付款，用户 A 确认付款。
   - 用户 A 指定 B 为取餐人。
   - B 按顺序推进取餐状态到 `DISTRIBUTED`，拼单主状态同步为 `FINISHED`。
   - 查询我的拼单和数据看板。
   - 用户 A logout。
   - 使用旧 `accessToken` 访问 `/api/auth/me`，确认被 Redis 黑名单拒绝。

   异常路径覆盖无 token 访问、重复加入、非发起人锁单、锁单后加入、非本人付款、非发起人确认付款、取餐状态跳跃和 logout 后旧 token 失效。

5. 验证 Controller、Token 校验、Service、Mapper、MySQL、Redis 完整链路

   集成测试的价值在于它不再只验证 Mockito 交互，而是验证实际运行路径：

   - Controller 路由、请求体绑定和统一响应结构。
   - `Authorization: Bearer <accessToken>` 的解析和失效判断。
   - Service 权限校验、状态流转和金额计算。
   - MyBatis-Plus Mapper 与 MySQL 表结构是否匹配。
   - `schema.sql` 在测试库中是否能支撑当前后端实体和 SQL。
   - Redis accessToken 黑名单 key 是否写入，以及 logout 后是否能阻断旧 token。

   这类测试比单元测试更慢、更依赖环境，但能发现单元测试发现不了的问题，例如数据库字段不一致、Controller 路径不一致、JSON 字段不匹配、Redis 不可用导致认证失败等。

6. 自动化测试与手动联调的分工

   自动化测试的价值：

   - 把核心业务规则变成可重复执行的回归基线。
   - 快速验证后端改动是否破坏登录、拼单、付款、取餐和看板。
   - 用断言固定金额、状态和权限边界，减少“靠页面点一点”的遗漏。
   - 通过集成测试证明 MVP 主流程在 MySQL 和 Redis 环境下可跑通。

   手动联调的价值：

   - 验证真实前端交互是否符合用户操作习惯。
   - 检查错误提示、按钮权限、页面刷新、路由跳转和视觉反馈。
   - 发现自动化测试不容易覆盖的体验问题，例如字段文案不清晰、操作入口不明显、移动端溢出。
   - 在多会话协作中确认前端实际调用的接口是否与 API 文档和后端实现一致。

   两者互补：自动化测试适合作为质量底线，手动联调适合发现产品体验和跨端集成问题。

7. 模型生成测试的不足

   LLM 能高效生成测试骨架、断言清单和异常场景，但仍存在局限：

   - 需要人工判断测试重点。模型容易生成大量字段级测试，但真正高风险的是金额一致性、状态机、权限和幂等。
   - 需要人工确认业务口径。比如 `EXPIRED` 是否属于 MVP、完成拼单走 `/status` 还是取餐状态 `DISTRIBUTED`，不能由模型猜测。
   - 需要人工审查断言是否过拟合实现。过度断言私有 SQL 片段或 Mapper 调用顺序，可能让测试变脆。
   - 需要人工维护测试环境。MySQL、Redis、测试库、schema 重建和测试数据隔离都必须由开发者确认。
   - 需要人工判断何时记录问题而不是修改业务代码。测试会话发现缺陷时只写报告，不跨角色直接修后端业务实现。

验证方式：

- 阅读 `backend/src/test/`，确认已有 `AuthServiceImplTest`、`GroupOrderServiceImplTest`、`MvpFlowIntegrationTest` 和 `application-test.yml`。
- 阅读 `docs/05-测试用例.md`，确认 MVP 回归基线记录为 `52 tests, 0 failures`，并已进入 V2 测试矩阵。
- 阅读 `docs/TASK_BOARD.md`，确认 T012-QA、BUG-QA-001、BUG-QA-002、T013-QA、T014-QA、T015-IT、T017-API-CONTRACT 等任务记录了测试发现、修复和收口过程。

结果：

测试会话通过 LLM 辅助建立了从 Service 单元测试到后端集成测试的质量网，既验证了认证、拼单、付款、取餐、我的拼单和看板主流程，也发现并推动修复了排序、状态口径、付款接口、API 文档字段和完成拼单接口等真实问题。最终 MVP 后端形成了可回归的 `52 tests, 0 failures` 基线，为前端联调和 V2 重构提供了稳定基础。

后续事项：

- V2 测试会话需要继续覆盖取消拼单、`EXPIRED` 超时关闭、履约事件、重复提交和并发冲突。
- 后续测试仍应优先围绕金额、权限、状态流转、幂等和跨服务链路设计，而不是单纯追求用例数量。
- 文档汇报会话可基于本记录补充演示材料，说明 LLM 在测试设计、缺陷发现和回归验证中的实际价值。

### 2026-05-29 SQL 会话阶段总结

日期：2026-05-29

参与方式：SQL 会话使用 LLM 辅助阅读领域模型、数据库设计、API 契约、`schema.sql`、`data.sql` 和任务看板，生成数据库设计初稿、初始化 SQL、演示数据、认证字段补丁和执行验证记录。

目标：总结 SQL 会话在 MySQL 表结构设计、初始化脚本生成、认证字段补充、核心表契约对齐和多会话协作中的实际作用、人工审查点与局限。

涉及文件：

- `docs/03-数据库设计.md`
- `sql/schema.sql`
- `sql/data.sql`
- `docs/TASK_BOARD.md`
- `docs/LLM辅助开发记录.md`

主要改动：

1. 根据领域模型设计 MySQL 表结构

   LLM 辅助从领域对象 `User`、`GroupOrder`、`OrderParticipant`、`MealItem`、`PaymentRecord`、`PickupRecord`、`OrderStatusLog`、`NotificationRecord` 推导出 MVP 表结构，并明确 MySQL 8 作为主数据源。初稿覆盖了 `user`、`group_order`、`order_participant`、`meal_item`、`payment_record`、`pickup_record`、`order_status_log`、`notification_record` 等表，建立了拼单主表与参与者表之间的 `1:N` 关系。

   人工审查重点确认了 `group_order` 是拼单聚合根，`order_participant` 是用户与拼单的参与关系表；同一用户不能重复加入同一拼单，需要 `group_order_id + user_id` 唯一约束；拼单发起人身份和参与者身份不能混淆，发起人如果也点餐，应在 `order_participant` 中有自己的参与记录。

2. 生成 `schema.sql` 和 `data.sql`

   LLM 辅助根据数据库设计生成 MySQL 8 初始化脚本和可演示数据。`schema.sql` 采用 InnoDB、`utf8mb4`、`decimal(10,2)` 金额字段、`varchar(32)` 状态字段，并为每张业务表保留 `create_time`、`update_time`。`data.sql` 覆盖已锁单/已下单奶茶拼单、待加入夜宵拼单、优惠分摊、付款标记、取餐状态和状态日志。

   人工审查发现初稿中部分增强能力容易被误解为 MVP 硬依赖，因此将 `notification_record` 标注为增强表，说明 MVP 主流程可不依赖通知记录。配送费和包装费也未单独建字段，按 MVP 口径并入成员原始金额或由发起人手动分摊，避免提前引入复杂费用模型。

3. 调整认证相关字段

   后端认证模块推进时，人工审查发现 `user` 表最初缺少密码哈希字段，导致注册和登录无法落库真实密码摘要，因此 SQL 会话补充 `password_hash varchar(255) not null`，并在设计文档说明只保存加密后的密码哈希，不存明文密码。后续认证升级为 `accessToken + refreshToken` 后，又补充 `last_login_time`、`last_login_ip`、`password_update_time` 和 `user_refresh_token` 表。

   `user_refresh_token` 表只保存 `token_hash`，不保存 refreshToken 明文；同时增加 `expire_time`、`revoked`、`revoked_time`，并为 `token_hash` 建唯一索引，为 `user_id + revoked`、`expire_time + revoked` 建查询索引。人工判断明确：accessToken 黑名单后续由 Redis 承担，SQL 表只负责 refreshToken 会话持久化。

4. 对齐核心表字段

   随着 API 和后端实现推进，人工审查发现发起拼单和取餐流程缺少取餐地点字段。SQL 会话随后补充 `group_order.pickup_location` 作为约定取餐地点，补充 `pickup_record.pickup_location` 作为实际取餐地点或履约阶段地点信息。该调整解决了 API 文档、后端创建拼单、取餐协同和前端展示之间的字段缺口。

   `payment_record` 和 `order_participant` 的关系也经过收口：当前付款状态保存在 `order_participant.payment_status`，付款标记和确认过程追加到 `payment_record`。`pickup_record` 保存当前取餐状态，关键状态推进追加到 `order_status_log`。这种设计让列表查询可以直接读取当前状态，同时保留过程记录供详情页和排查使用。

5. 逻辑外键、索引和金额字段设计

   SQL 会话采用“不使用物理外键，保留逻辑外键说明”的策略。原因是课程项目和多会话并行开发阶段更需要易初始化、易重建、便于演示和联调；数据一致性主要由后端业务校验和事务保证。表结构仍按逻辑依赖排序，并在关联字段上建立必要索引。

   索引设计围绕实际查询场景展开：`group_order.status + deadline_time` 用于大厅筛选和过期处理，`group_order.status + create_time` 用于大厅最新拼单展示，`creator_id + create_time` 用于“我发起的拼单”，`order_participant.group_order_id + user_id` 用于防重复加入，付款和取餐表也围绕订单、用户、状态和时间建立索引。金额字段统一 `decimal(10,2)`，后端对应 `BigDecimal`，避免浮点误差。

人工审查发现的问题：

- 初版数据库设计覆盖了主要实体，但认证登录缺少 `password_hash`，无法支撑真实注册/登录。
- 初版发起拼单字段缺少 `pickup_location`，不能满足取餐地点展示和履约协同。
- `notification_record` 容易被误判为 MVP 必需表，后续明确为增强表。
- 配送费、包装费如果单独建字段会扩大 MVP 金额模型，因此改为并入成员原始金额或手动分摊。
- token 存储口径需要多次收口，最终明确 refreshToken 不明文落库，只保存 hash；accessToken 黑名单由 Redis 负责。
- 数据库设计文档、API 文档和 `schema.sql` 在多轮迭代中存在阶段性不同步，需要通过任务看板和交接摘要逐步收口。

MySQL 8 执行验证情况：

- `schema.sql` 和 `data.sql` 曾在 MySQL 8.4.9 环境、数据库 `campus_haofan` 中执行通过。
- 已验证核心表包括 `user`、`group_order`、`order_participant`、`meal_item`、`payment_record`、`pickup_record`、`order_status_log`、`notification_record`。
- 后续新增认证升级字段和 `user_refresh_token` 后，SQL 会话完成了静态校验和文档同步；如本地数据库已执行旧版初始化脚本，需要重新执行最新 `schema.sql`、`data.sql`，或编写迁移脚本保留历史数据。

SQL 会话在多会话协作中的作用：

- 为后端实体、Mapper、DTO/VO 提供字段来源。
- 为 API 会话提供字段映射依据，例如 `username` 映射 `user.account`、金额字段和状态字段返回口径。
- 为测试会话提供可演示初始化数据，便于覆盖锁单、分摊、付款、取餐和状态日志。
- 在发现数据库字段缺失时解除后端阻塞，例如补充 `password_hash`、`pickup_location`、`user_refresh_token`。
- 通过任务看板记录 SQL 任务完成、阻塞解除和后续依赖，使后端、前端、测试会话可以按依赖顺序推进。

局限：

- LLM 能快速生成表结构初稿和 SQL 脚本，但不能替代人工审查业务边界、字段必要性和跨文档一致性。
- 初稿容易倾向“一次性补全”，需要人工压缩 MVP 范围，避免把增强能力做成硬依赖。
- SQL 会话只负责数据库设计和脚本，不实现后端状态机、金额计算、权限校验或 token 签发逻辑。
- 对已运行数据库，直接重跑初始化脚本会清空数据；生产或保留数据场景必须另行编写迁移脚本。

验证方式：

- 阅读 `docs/03-数据库设计.md`、`sql/schema.sql`、`sql/data.sql` 和 `docs/TASK_BOARD.md`。
- 静态检查字段类型、索引、状态字段、金额字段和演示数据。
- 参考 MySQL 8.4.9 执行验证记录确认 MVP 初始化脚本曾执行通过。

结果：

SQL 会话通过 LLM 辅助完成了从领域模型到 MySQL 表结构、初始化脚本、演示数据和认证升级字段的持续收口。人工审查补齐了认证、取餐地点、增强表边界、费用处理和 token 存储策略，使数据库设计能够支撑 MVP 主流程和后续认证升级。

后续事项：

- V2-T002 需要把当前 V2 数据库设计建议落地到 `schema.sql` 和 `data.sql`。
- 如需保留历史数据，应新增迁移脚本，而不是直接重跑初始化脚本。
- 后续 SQL 变更仍需同步数据库设计文档、API 文档、后端实体和测试用例。
