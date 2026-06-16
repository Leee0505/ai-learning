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