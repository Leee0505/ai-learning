## 项目概况

### 业务定位
企业级工单系统（Ticket Management System），类似 Zendesk 或 飞书审批的简化版，支持客服团队高效处理用户请求。

### 核心价值
- 用户：快速提交问题，实时跟踪处理进度
- 客服：统一工作台，高效处理和分配工单
- 管理员：数据驱动决策，服务质量监控

### 用户角色（3种）
| 角色 | 权限范围 | 典型操作 |
|------|---------|---------|
| 普通用户 (ROLE_USER) | 仅自己的工单 | 提交、查看、回复、关闭工单 |
| 客服 (ROLE_AGENT) | 所有未关闭工单 | 接单、处理、转派、添加内部备注 |
| 管理员 (ROLE_ADMIN) | 全系统 | 用户管理、工单分配策略、SLA配置、全量导出 |

### 核心业务流程


### 功能模块清单（MVP v1.0）

**用户端（6个页面）**
- 仪表盘：我的工单统计（进行中/已完成/平均响应时间）
- 新建工单：表单（标题、分类、优先级、描述、附件上传）
- 工单列表：分页表格（支持按状态/时间筛选）
- 工单详情：时间线展示（用户消息+客服回复+系统消息）
- 个人中心：修改密码、通知设置（邮件/WebSocket）
- 消息通知：实时未读红点（WebSocket）

**客服端（5个模块）**
- 工作台：待接单队列、我的进行中工单、超时告警
- 工单查询：全局搜索（ES）+ 高级筛选（按标签、优先级、创建时间）
- 工单处理：标准回复模板、添加内部备注（用户不可见）、转派给其他客服
- 知识库：常见问题快捷回复（Markdown 支持）
- 绩效看板：今日完成数、平均处理时长、满意度评分

**管理端（4个模块）**
- 用户管理：增删改查、角色变更、启用/禁用账号
- 工单配置：自定义字段、优先级定义、SLA 规则（如：紧急工单4小时响应）
- 系统监控：RabbitMQ 队列深度、Redis 命中率、API 响应时间
- 数据导出：按时间范围导出工单（Excel/CSV），异步生成（Kafka 消费）

### 非功能性需求
- 性能：核心 API 响应 < 200ms（P95），缓存命中率 > 80%
- 安全：JWT 无状态认证，密码 bcrypt 加密，操作审计日志（记录谁在什么时候改了啥）
- 并发：支持 1000+ 在线用户，工单创建 TPS > 100
- 可观测性：ELK 日志聚合（可选），Prometheus + Grafana 监控（后续）
- 数据一致性：关键操作使用 @Transactional，消息队列保证至少一次投递


## 技术约束

### 后端核心栈
- **框架**: Spring Boot 3.2.x（最新 LTS），Spring Security 6.x
- **ORM**: MyBatis-Plus 3.5.x（配合代码生成器）
- **数据库**: MySQL 8.0.x（主库，读写分离后续）
  - 核心表：user, ticket, ticket_reply, ticket_attachment, audit_log
  - 分页策略：物理分页（MyBatis-Plus Page）
- **缓存**: Redis 7.x（Redisson 客户端）
  - 缓存策略：用户会话（TTL 30min）、工单列表（TTL 5min）、计数器（不设 TTL）
  - 数据结构：String（简单 KV）、Hash（用户信息）、Set（在线客服）、Sorted Set（排行榜）
- **消息队列**: Kafka 3.x（或 Kafka，你写了 ES 但应该是 MQ？）
  - Topic 设计：
    - `ticket.created`：新工单通知客服
    - `ticket.assigned`：工单分配事件
    - `notification.email`：异步发送邮件
    - `audit.log`：操作日志持久化
  - 分区策略：按工单 ID hash（保证顺序消费）
- **搜索引擎**: Elasticsearch 8.x（可选，v1.0 先用 MySQL 全文索引）
  - 如果包含 ES：索引 `tickets` 存储工单标题+描述+回复内容，支持中文分词（ik_smart）
- **定时任务**: Spring @Scheduled（单机），后续可升级 XXL-JOB
  - 任务清单：
    - 每5分钟：检测超时未分配工单 → 升级通知主管
    - 每小时：同步 MySQL -> ES（如果需要）
    - 每天凌晨2点：清理 90 天前已关闭工单（归档到历史表）
- **unit test：任何新增接口逻辑和修改接口逻辑都需要添加对应的unit test，并保证测试的可维护性

### 前端核心栈
- **框架**: Vue 3.4+（Composition API + `<script setup>`）
- **构建**: Vite 5.x
- **语言**: TypeScript 5.x（严格模式）
- **UI 库**: Element Plus 2.x（优先使用，减少手写 CSS）
- **状态管理**: Pinia 2.x（配合持久化插件）
- **HTTP 客户端**: Axios 1.x（拦截器处理 JWT 自动刷新）
- **实时通信**: WebSocket（原生 API 或 Socket.IO-client）
- **工具库**: Day.js（时间处理）、ECharts（仪表盘图表）、Vditor（Markdown 编辑器）

### 架构约束
- **通信协议**: RESTful API（JSON）+ WebSocket（实时通知）
- **API 规范**: 统一响应格式 `{ code: 200, message: "success", data: T }`
- **认证授权**: JWT（Access Token 2h + Refresh Token 7d），存 Redis 黑名单机制
- **文件存储**: 本地存储（开发）/ 阿里云 OSS 或 MinIO（生产），限制单文件 10MB
- **部署方式**: Docker Compose 一键启动（MySQL+Redis+Kafka+Backend+Frontend+Nginx）

## 校验规范（核心）

### 校验设计理念
**目标**：让校验逻辑成为业务代码的"第一道防线"，而非事后补救。

**核心原则**：
1. **分层校验**：前端（体验）→ DTO（格式）→ Service（业务）→ DB（兜底）
2. **策略驱动**：根据字段的业务敏感度决定校验强度，而非一刀切
3. **思考前置**：AI 生成代码前必须输出校验思考过程

### 校验强度矩阵（AI 生成代码的决策依据）

| 强度级别 | 适用场景 | 校验内容 | 错误处理 | 示例字段 |
|---------|---------|---------|---------|---------|
| 🔴 **严格** | 用户身份、联系方式、敏感数据 | 非空 + 格式 + 业务唯一性 + 外部验证（短信/邮件） | 立即拒绝，返回明确错误码 | 邮箱、手机号、密码、身份证号 |
| 🟡 **标准** | 核心业务字段 | 非空 + 格式 + 长度范围 + 业务规则（如枚举值） | 立即拒绝，返回提示信息 | 工单标题、优先级、分类、状态 |
| 🟢 **宽松** | 描述性、备注性文本 | 仅非空 + 极大长度限制（如 10000 字符） | 允许通过，记录警告日志 | 工单描述、备注、回复内容 |
| ⚪ **自定义** | 业务特定字段 | 根据业务规则动态判断（如 VIP 用户的优先级范围放宽） | 动态处理 | 不同角色的可见字段、动态表单字段 |


## 工作语言规范

### 对话与文档
- **日常对话**: 中文（与 AI 交互使用中文）
- **项目文档**: 中文（README、API 文档、部署手册、数据库设计文档）
- **代码注释**: 英文（类/方法/复杂逻辑用英文注释）
- **Git 提交**: 英文（格式：`feat: add ticket creation API`）

### 代码注释示例
```java
// Good - English comment
// Cache user info for 30 minutes to reduce DB pressure
redisTemplate.opsForValue().set("user:" + userId, user, 30, TimeUnit.MINUTES);

// Bad - Chinese comment
// 缓存用户信息30分钟


## 后续演进计划（Post-MVP）

### 多租户架构 (v1.1)

**目标：** 支持多个组织（公司/团队）独立使用同一套系统，数据完全隔离。

**方案：共享数据库 + tenant_id 列隔离**（MVP 阶段无需改动）

| 阶段 | 改动 | 预估 |
|------|------|------|
| **Phase 1 — 数据模型** | 新增 `tenant` 表（id, name, slug, status, created_date）。user, ticket, ticket_reply, ticket_attachment, reply_template 添加 `tenant_id` 列（默认 NULL = 全局/迁移前数据）。所有唯一索引加 tenant_id 前缀。 | 4h |
| **Phase 2 — 认证隔离** | 注册/登录时绑定 tenant。JWT 加 `tenant_id` claim。SecurityContext 自动解析当前租户。同一 email 可在不同 tenant 注册。 | 3h |
| **Phase 3 — 数据隔离** | MyBatis-Plus 分页插件拦截 SQL 自动加 `tenant_id = ?` 条件（行级隔离）。Admin 可查看全 tenant 数据。 | 2h |
| **Phase 4 — 模板分级** | `tenant_id = NULL` → 系统默认模板（所有租户可见）。`tenant_id = xxx` → 租户自定义模板（仅本租户可见）。| 1h |

**Template 分级设计：**
```
系统默认模板（tenant_id = NULL）
  ↓ 所有租户可见，不可编辑
租户 A 自定义模板（tenant_id = 1）
租户 B 自定义模板（tenant_id = 2）
  ↓ 各自独立，互不可见
```

**注意：** 多租户不在 MVP 范围内。当前阶段所有数据全局共享，待 MVP 完成并验证业务模型后再实施。

### 短信验证 & 手机号绑定 (v1.2)

**目标：** 验证手机号为本人所有，提升账户安全性，解锁手机号登录能力。

**现状：** `phone` 字段为可选辅助信息，无校验、无验证。`AdminCreateUserRequest` 仅 `@Size(max=32)`。

**方案：** 接入阿里云短信服务 / Twilio SMS

| 阶段 | 改动 | 预估 |
|------|------|------|
| **Phase 1 — 短信服务接入** | 接入阿里云短信 SDK（`dysmsapi`）或 Twilio。申请短信签名 + 验证码模板。封装 `SmsService` 统一接口。 | 3h |
| **Phase 2 — 验证码基础设施** | Redis 存储：`sms:code:{phone}`（TTL 5min）+ `sms:rate:{phone}`（60s 间隔）。60s 发送频率限制、单日上限 10 条、5 次错误失效。 | 2h |
| **Phase 3 — 验证触发点** | `POST /api/auth/send-sms-code`（发送验证码）。注册时可选绑定手机 + 验证码。Profile 页面新增"绑定/更换手机号"入口。Admin 创建用户时可选绑定手机。 | 3h |
| **Phase 4 — 手机号登录** | 支持手机号 + 验证码登录（除邮箱+密码和用户名+密码外的第三种方式）。`LoginRequest.login` 自动识别手机号格式。 | 2h |
| **Phase 5 — 短信通知** | 工单状态变更（分配、解决、关闭）推送到绑定手机。`notification.sms` Kafka topic。 | 2h |

**校验升级：**

| 字段 | 当前 | 升级后 |
|------|------|--------|
| `phone`（注册/Profile） | 🟢 宽松 `@Size(max=32)` | 🔴 严格：`@Pattern` 手机号格式 + SMS 验证码校验 |
| `phone`（Admin 创建） | 🟢 宽松 `@Size(max=32)` | 🟡 标准：`@Pattern` + 可选 SMS 验证 |

**注意：** 手机号验证不在 MVP 范围内。MVP 阶段 phone 仍为可选辅助字段。待多租户 (v1.1) 完成后再实施。