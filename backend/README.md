# 狠狠学后端

## 模块

```text
backend
├── common
│   ├── common-core             异常、响应、分页、审计和序列化约定
│   ├── common-web              Servlet HTTP 适配通用能力
│   └── common-observability    traceId、指标和审计日志
├── api
│   └── api-{auth,content,learning,question,file,ai}
│       └── 仅 Dubbo DTO、服务接口、稳定枚举；禁止持久化实体
└── services
    ├── gateway-service         统一入口、Sa-Token、限流、路由
    ├── auth-service            身份、用户资料、RBAC、邮箱验证码
    ├── content-service         博客文章、分类、标签、收藏
    ├── learning-service        知识树、版本、节点、学习对话
    ├── question-service        题库、导入、练习与统计
    ├── file-service            MinIO 上传/下载授权和对象元数据
    └── ai-service              凭据、模型路由、Prompt、AI 任务
```

每个业务服务（Gateway 除外）采用相同的包边界：

```text
com.hengxue.<service>
├── application       用例编排、命令/查询处理
├── domain            聚合、领域服务、领域事件和仓储接口
├── infrastructure
│   ├── persistence   MyBatis-Plus 实体、Mapper、仓储实现
│   ├── messaging     RocketMQ / Outbox 生产与消费适配
│   └── rpc           Dubbo 消费端适配
├── interfaces
│   ├── rest          HTTP Controller、请求/响应 DTO
│   └── dubbo         对外 Dubbo 服务实现
└── config            Spring、Sa-Token、消息、任务配置
```

数据库迁移位于每个服务的 `src/main/resources/db/migration/`，仅追加 Flyway 迁移文件。所有 `application.yaml` 中的 `TODO` 为部署前必须检查或填写的参数，机密值只能从环境变量或密钥管理系统注入。

## 验证

```bash
cd backend
mvn validate
```

项目已通过 `.mvn/settings.xml` 配置阿里云公共仓库镜像。进入 `backend/` 后直接执行 Maven 命令即可生效，无需更改本机全局 `~/.m2/settings.xml`。
