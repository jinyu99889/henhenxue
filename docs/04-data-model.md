# 数据库建库说明

数据库的唯一结构来源是 [`04-schema.sql`](04-schema.sql)。该脚本可在 MySQL 8.0 空实例直接执行，所有表、字段、生成列、索引和字段含义均以 SQL `COMMENT` 写入。

```sh
mysql -u <user> -p < docs/04-schema.sql
```

脚本会创建 `hengxue` schema，字符集为 `utf8mb4`、排序规则为 `utf8mb4_0900_ai_ci`。它故意不创建跨服务外键：服务边界内由本地事务校验，跨服务 ID 仅为逻辑引用。

投入应用时按表归属配置写入权限，但统一平台表不再按服务复制。第一期使用一套数据库迁移基线：

| 服务 | 初始表 |
| --- | --- |
| `ai-service` | `sys_ai_credential`、异步任务 |
| `auth-service` | 用户、身份、RBAC、审计 |
| `file-service` | `sys_file_object` |
| `content-service` | `content_*` |
| `learning-service` | `learn_*` |
| `question-service` | `question_*`、`practice_*` |
| 统一平台 | `sys_outbox_event`、`sys_idempotency_record`；用服务类型字段区分写入者 |

后续变更只能新增 Flyway 迁移，不能直接修改已执行的基线。未来只按业务 ID 做水平分表/分库，不按服务垂直拆分；字段需要查询、排序、关联、唯一性或权限校验时，必须新增实体列或关联表，不能塞进 JSON。
