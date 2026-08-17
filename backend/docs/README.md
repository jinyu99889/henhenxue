# 狠狠学后端开发文档

本目录与 Maven Reactor 同级，便于在 IDEA 单独打开 `backend/` 时直接查看和维护接口与数据契约。实施接口、数据库或迁移前，应先更新对应契约。

| 文档 | 作用 |
| --- | --- |
| [03-api-specification.md](03-api-specification.md) | HTTP 全局约定、任务状态机、错误码与并发规则 |
| [openapi.yaml](openapi.yaml) | 可机器校验的 OpenAPI 3.1 唯一接口契约 |
| [03-api-reference.md](03-api-reference.md) | 根据 OpenAPI 自动生成的端点开发手册 |
| [04-schema.sql](04-schema.sql) | MySQL 8.0 一期建库基线，字段、索引和说明的唯一来源 |
| [04-er-diagram.md](04-er-diagram.md) | 按服务分区的实体关系图与逻辑引用说明 |
| [04-data-model.md](04-data-model.md) | 建库基线和后续人工 SQL 变更说明 |

修改 [`openapi.yaml`](openapi.yaml) 后，在仓库根目录执行：

```sh
ruby scripts/generate_api_reference.rb
```

该命令会更新本目录下的 `03-api-reference.md`。
