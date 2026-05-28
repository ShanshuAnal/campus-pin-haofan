# frontend/AGENTS.md

## 适用范围

本文件适用于前端会话和任何修改 `frontend/` 的任务。不要在本目录下生成后端代码、SQL 表结构或文档汇报材料。

## 技术栈

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Axios
- Vue Router

## 目录结构

前端工程创建后建议采用：

```text
frontend/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/
    ├── assets/
    ├── components/
    ├── layouts/
    ├── router/
    ├── stores/
    ├── types/
    ├── utils/
    └── views/
```

不要在当前仅要求文档拆分的任务中创建工程脚手架、`package.json`、Vue 页面或组件。

## 页面结构

MVP 页面建议覆盖：

- 登录页；
- 拼单大厅页；
- 发起拼单页；
- 拼单详情页；
- 我的拼单页；
- 取餐协同相关视图；
- 简单数据看板页。

页面应服务于主流程演示，不要过度设计动画和复杂交互。

## API 封装规则

- API 请求统一放在 `src/api/`。
- Axios 实例、拦截器、错误处理集中管理。
- 不在页面组件中硬编码后端基础地址。
- 请求和响应类型放在 `src/types/` 或对应模块类型文件中。
- 页面组件不直接拼装复杂请求逻辑，应调用 API 封装函数。
- 接口路径、字段、错误码必须以 API 文档为准。

## Mock 数据规则

- 前端 mock 数据必须与 API 文档一致。
- Mock 字段名、状态枚举、金额格式必须与后端约定一致。
- Mock 只用于前端开发和演示，不得反向定义接口契约。
- API 文档变更后，必须同步更新相关 mock 数据和类型定义。
- 某模块完成真实接口联调后，必须删除该模块对应的 mock 数据和 mock API，后续不得继续通过 mock 兜底该模块主流程。
- 金额展示应按两位小数处理，前端不得承担核心金额分摊计算。

## 状态管理

- 用户登录态使用 Pinia 管理。
- 拼单列表、拼单详情、付款状态和取餐状态可按模块拆分 store。
- Store 不替代后端状态校验。
- 前端按钮状态只能改善体验，不能作为业务安全边界。

## 前端命令

前端工程创建后，优先使用以下命令：

```powershell
cd frontend
npm install
npm run dev
npm run build
```

如项目配置了类型检查或测试脚本，应补充执行：

```powershell
cd frontend
npm run type-check
npm run test
```
