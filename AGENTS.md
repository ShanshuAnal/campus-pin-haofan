# AGENTS.md

## 项目定位

校园拼好饭系统是面向高校学生的拼单、凑单、金额分摊、付款标记与取餐协同系统。

系统不是外卖平台，不接入真实外卖商家接口，不接入真实支付接口，重点实现校园内部拼单协同流程。

## 技术栈总览

后端：

- Java 21
- Spring Boot 3.2.x
- MyBatis-Plus
- MySQL 8

前端：

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Axios
- Vue Router

中间件：

- Redis 和 RocketMQ 只作为增强能力。
- Redis 和 RocketMQ 不能作为 MVP 主流程硬依赖。
- MVP 主流程必须在仅依赖 MySQL 的情况下可运行。

## 当前 MVP 主流程

```text
登录
-> 发起拼单
-> 成员加入
-> 凑单计算
-> 锁定拼单
-> 优惠分摊
-> 标记付款
-> 指定取餐人
-> 更新取餐状态
-> 完成拼单
```

任何任务都不能破坏这条主流程。

## 多会话角色边界

- 主控会话：负责需求、任务拆分、接口约定、数据库设计和协作协调。
- 后端会话：负责 `backend/`，遵守 [backend/AGENTS.md](backend/AGENTS.md)。
- 前端会话：负责 `frontend/`，遵守 [frontend/AGENTS.md](frontend/AGENTS.md)。
- SQL 会话：负责 `sql/` 和数据库设计同步，遵守 [sql/AGENTS.md](sql/AGENTS.md)。
- 文档汇报会话：负责文档、演示材料和 LLM 记录，遵守 [docs/AGENTS.md](docs/AGENTS.md)。
- 测试会话：负责测试用例、接口验证、Bug 复现和回归检查。

详细协作流程见 [docs/CODEX_WORKFLOW.md](docs/CODEX_WORKFLOW.md)。

## 文件修改边界

- `backend/` 主要由后端会话修改。
- `frontend/` 主要由前端会话修改。
- `sql/` 主要由 SQL 会话或后端会话修改。
- `docs/` 主要由主控、测试、文档汇报会话修改。
- 根目录 `AGENTS.md` 只保留全局规则和索引。
- 不要生成与当前任务无关的业务代码、工程脚手架或配置文件。
- 不要覆盖其他会话或用户的未提交改动。

## 接口和数据库变更

- 接口变更必须先更新 API 文档，再修改前后端代码。
- 数据库变更必须先更新数据库设计文档和 SQL 规则文件，再修改后端实体、Mapper、DTO、VO。
- 修改接口或数据库时，必须在交接摘要中说明影响范围和验证方式。
- 发现接口或数据库冲突时，暂停实现并报告，不要自行猜测合并。

## 每次任务开始前必须阅读

1. 根目录 `AGENTS.md`。
2. 当前领域的 `AGENTS.md`，例如 `backend/AGENTS.md` 或 `frontend/AGENTS.md`。
3. [docs/CODEX_WORKFLOW.md](docs/CODEX_WORKFLOW.md)。
4. [docs/TASK_BOARD.md](docs/TASK_BOARD.md)。
5. 当前任务相关的需求、API、数据库或测试文档。

如果 `docs/TASK_BOARD.md` 不存在，先创建任务看板模板，不要直接开始 coding。

## 每次任务结束后的交接摘要

交接摘要必须包含：

```text
本次完成：
修改/新增文件：
验证方式：
未完成事项：
是否影响接口：
是否影响数据库：
是否影响其他会话：
下一步建议：
```

未执行验证时必须说明原因。

## 规则索引

- 后端规则：[backend/AGENTS.md](backend/AGENTS.md)
- 前端规则：[frontend/AGENTS.md](frontend/AGENTS.md)
- SQL 规则：[sql/AGENTS.md](sql/AGENTS.md)
- 文档规则：[docs/AGENTS.md](docs/AGENTS.md)
- 多会话流程：[docs/CODEX_WORKFLOW.md](docs/CODEX_WORKFLOW.md)
- 任务看板：[docs/TASK_BOARD.md](docs/TASK_BOARD.md)
- 项目说明：[README.md](README.md)


### git规则
每个任务完成后，Codex 必须输出建议 commit message，但不得自动执行 git add、git commit、git push。提交由用户确认后执行。