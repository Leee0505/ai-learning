# DB Seed — 数据库测试数据生成脚本

清空数据库并生成多租户测试数据。

## 服务器部署

将整个 `db-seed/` 文件夹拷贝到服务器上任意目录，然后执行：

## 快速开始

```bash
# 1. 安装依赖（只需一次）
pip install pymysql

# 2. 运行（按需修改参数）
python seed_db.py \
  --host 127.0.0.1 \
  --port 3306 \
  --user root \
  --password jw-0505 \
  --database ticket_db \
  --tenants 3 \
  --agents 4 \
  --users 8 \
  --tickets 10000 \
  --no-confirm
```

## 参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `--host` | 127.0.0.1 | 数据库地址 |
| `--port` | 3306 | 数据库端口 |
| `--user` | root | 数据库用户 |
| `--password` | root | 数据库密码 |
| `--database` | ticket_system | 数据库名 |
| `--tenants` | 3 | 租户数量 |
| `--agents` | 4 | 每租户 agent 数量 |
| `--users` | 8 | 每租户普通用户数量 |
| `--tickets` | 10000 | 每租户 ticket 数量 |
| `--no-confirm` | (无) | 跳过确认提示 |

也可直接改脚本顶部 `CONFIG` 字典的默认值，然后不带参数运行。

## 生成内容

| 步骤 | 内容 |
|------|------|
| 清空 | 按 FK 顺序 TRUNCATE 全部 20 张表 |
| 租户 | N 个 tenant |
| 用户 | 1 个 superadmin（tenant_id=NULL）+ 每租户 1 admin + M agents + K users |
| 配置 | 5 个 reply templates + 4 个 knowledge articles + 4 个 SLA configs + 2 个 survey templates |
| 工单 | 每租户 X 条 ticket，每条 1-4 条 reply |

## 生成账号

| 角色 | 命名规则 | 密码 |
|------|---------|------|
| superadmin | `superadmin` | `Admin@123` |
| 租户 admin | `admin1`, `admin2`... | `Admin@123` |
| 租户 agent | `agent1-1`, `agent1-2`... | `Agent@123` |
| 租户 user | `user1-1`, `user1-2`... | `User@123` |

## 示例

```bash
# 小规模测试
python seed_db.py --tenants 1 --agents 2 --users 3 --tickets 50

# 大规模压测（跳过确认）
python seed_db.py --tenants 5 --tickets 50000 --no-confirm

# 远程数据库
python seed_db.py --host 192.168.1.100 --user admin --password xxx --database prod_test
```
