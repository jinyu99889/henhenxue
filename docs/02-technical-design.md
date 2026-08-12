# 狠狠学技术设计文档

## 1. 技术基线

| 层级 | 选型 | 说明 |
| --- | --- | --- |
| JDK | Java 17 | 统一编译与运行时版本。 |
| 微服务 | Spring Boot 3.5.x + Spring Cloud 2025.0.x + Spring Cloud Alibaba 2025.0.x | 该组合满足 Java 17；版本号在项目初始化时锁定到具体补丁版本。 |
| 服务治理 | Nacos | 服务注册、发现和配置中心。 |
| 网关与保护 | Spring Cloud Gateway + Sentinel | Gateway 统一路由；Sentinel 处理限流、熔断和热点保护。 |
| RPC | Dubbo 3.x | 服务间业务契约调用，禁止绕过网关暴露内部服务。 |
| 认证 | Sa-Token 1.44.x | 网关做登录和路由权限校验，下游校验 Same-Token。 |
| AI | AgentScope Java 2.x | 流式模型调用、会话隔离、工具调用与可观测性；仅适配 DeepSeek 和阿里云百炼（DashScope），模型 Key 由用户提供。 |
| 数据 | MySQL 8.0、Redis 7.x、MinIO | 关系数据、缓存/会话/限流、对象存储。 |
| 消息 | RocketMQ 5.x | 导入、AI 后处理、统计等异步事件。 |
| 任务 | XXL-JOB | 发布、补偿、过期清理和统计任务。 |
| 前端 | React 19 + JavaScript + Vite + Motion + Lucide React + plain CSS | 单独应用，通过 Gateway 的 BFF 风格 API 访问；后续业务路由、状态和 HTTP 层按页面需求引入。 |

邮件验证码由 `auth-service` 使用用户配置的 QQ 邮箱 SMTP 发送。SMTP 主机固定为 `smtp.qq.com`，使用 SSL/TLS 与 QQ 邮箱“授权码”；授权码作为部署密钥注入，禁止写入 Nacos 明文配置、数据库或日志。

说明：`sentence` 指 Sentinel，`rpckermq` 指 RocketMQ。当前方案不引入 Seata，跨服务最终一致性使用 Outbox 事件。

## 2. 架构与服务边界

```text
React SPA
    |
Spring Cloud Gateway (Sa-Token, rate limit, request id)
    |
    +-- auth-service       登录、身份、角色和令牌
    +-- user-service       用户资料、偏好
    +-- content-service    博客文章、分类、标签、收藏
    +-- learning-service   知识树、版本、节点、学习对话
    +-- question-service   题库、题目、导入、练习与统计
    +-- file-service       MinIO 上传凭证、对象元数据、下载授权
    +-- ai-service         AgentScope、模型路由、Prompt 模板、AI 任务

Nacos / Sentinel / MySQL / Redis / MinIO / RocketMQ / XXL-JOB
```

采用 Maven 多模块单仓库：`common-*`（异常、响应、审计、序列化）、`api-*`（Dubbo DTO/接口）、各服务应用。禁止把 JPA/MyBatis Entity 放入 `api-*`；跨服务只传 DTO 与稳定枚举。

第一期可用 `content-service`、`learning-service`、`question-service`、`file-service`、`ai-service` 五个业务服务启动，用户和认证先合并为 `auth-service`。服务边界保持不变，后续可拆 `user-service`，不改变 API 前缀或数据归属。

## 3. 调用与一致性原则

- 浏览、写入等同步请求走 Gateway HTTP API；业务服务之间的实时查询优先 Dubbo。
- AI 任务和 MinerU ZIP 导入由 HTTP 创建，后台消费执行，进度通过 SSE 推送。
- 每个服务只写自己的 schema/数据库表；严禁跨服务直接写表。
- 对“已提交的数据库事务需要发消息”的场景写入 `sys_outbox_event`，由投递器可靠发送至 RocketMQ；消费者按事件 ID 去重。
- RocketMQ 事务消息仅在生产者本地事务与消息半消息协调确有必要时使用。它不能解决两个业务服务的强一致提交。
- 博客搜索第一期使用 MySQL InnoDB FULLTEXT 索引，不引入 Elasticsearch；仅检索当前已发布文章的最新版本。MySQL 启动配置设为 `ngram_token_size=2`，全文索引显式使用 `WITH PARSER ngram` 以支持中文检索。

## 4. AI 设计

`ai-service` 内置 DeepSeek 与阿里云百炼（DashScope）两个固定提供商适配器及其官方 API 地址，不接受用户传入的 `Base URL`。用户只选择提供商、输入 API Key 和模型名；保存接口使用该 Key 和模型名发起最小非流式聊天请求做连通性校验。认证失败、网络超时、服务端错误或模型不存在时，接口返回 `AI_CREDENTIAL_CONNECTION_FAILED` 和脱敏后的可展示原因，且不保存凭据。

校验成功后，API Key 使用信封加密保存：业务数据库仅保存密文、KMS/环境注入的主密钥标识和 Key 指纹；仅 `ai-service` 在实际调用的短生命周期内解密。接口只返回脱敏后的末四位和验证状态，管理员、审计日志、异常信息和消息体不得包含明文 Key。

`learning-service` 将受控的任务参数和 `credentialId` 传给 `ai-service`，不直接依赖模型 SDK。知识树创建时保存用户选定的默认凭据，叶子节点追问默认使用该凭据；用户可在树设置中切换。题库 MinerU ZIP 导入必须显式提供凭据。每个 AI 任务只能使用任务发起用户拥有且处于 `VERIFIED` 状态的凭据；任务记录保存凭据 ID 和指纹快照用于审计，绝不保存密钥。DeepSeek 使用其 OpenAI 兼容聊天接口；DashScope 使用其 Java SDK/官方聊天接口，两者在 `ai-service` 内被统一为流式聊天能力。

AI 调用只对连接超时、HTTP 429 和提供商 5xx 进行最多两次指数退避重试；认证失败、模型不存在、余额不足、参数校验失败和已经收到流式内容后的异常均不重试。重试耗尽后任务失败并向用户反馈，不自动切换到其他凭据或模型。

AgentScope Java 的生产调用应复用单例 Agent，通过 `RuntimeContext(userId, sessionId)` 隔离并发会话；WebFlux 链路保持响应式，业务服务中不调用阻塞式 `.block()`。学习树生成、叶子节点追问和题目解析分别使用不同的系统 Prompt、工具权限和 Token 上限。追问提交前，`learning-service` 必须以请求的 `versionId` 查询节点，并确认该节点不存在子节点；根节点或非叶子节点直接返回业务错误，不创建会话、AI 任务或 RocketMQ 消息。

模型输出必须同时通过以下校验：

1. Markdown 标题层级语法校验。
2. 最大节点数、最大深度、最大单节点长度校验。
3. 解析后树只有一个根、没有孤立节点或循环。
4. 敏感内容和提示注入检测。

标题解析器只把 ATX 标题映射为节点。每个 `learn_node.content_md` 仅保存标题后的直属正文；节点详情读取时由 `learning-service` 按 `parent_node_key` 和 `sort_order` 深度优先聚合子树，并在输出中恢复标题层级。根节点返回完整文档，非叶子节点返回该节点子树，叶子节点只返回直属正文。前端只消费该读取结果，不实现 Markdown 树解析。

校验失败时任务进入 `FAILED`，原始输出仅向创建者和管理员可见，用于重试诊断。

## 5. 鉴权与安全

- 登录由 `auth-service` 签发 Sa-Token；Gateway 使用 Reactor Filter 做登录、角色和接口白名单校验。
- Gateway 为下游服务附加 Same-Token；下游 Servlet 服务必须验证该令牌，拒绝绕过 Gateway 的外部调用。
- 资源权限使用“所有者或管理员”策略，不能仅依赖前端隐藏按钮。
- MinIO 不公开桶。上传使用用途、大小、MIME 类型受限的预签名 URL；下载按对象归属重新签名。
- 博客资源仅管理员可上传，业务层不限制附件类型；对 HTML、SVG、脚本、可执行文件等潜在活动内容一律以 `Content-Disposition: attachment` 下载，禁止同源内联执行。
- 密码使用 Argon2id 或 BCrypt，不记录令牌、密码、完整 Prompt 中的敏感个人信息到日志。
- 账号密码登录；注册与密码重置请求必须携带邮箱验证码。验证码存 Redis 并设置短 TTL、单次消费和按邮箱/IP 限流。管理员初始化数据仅由数据库迁移/受控运维脚本写入，不提供公共角色授予接口。
- RBAC 使用“用户-角色-权限”模型：Gateway 路由和服务方法均按权限码校验，`ADMIN` 角色只是在初始化时被授予完整后台权限的预置角色。
- 修改操作携带 `Idempotency-Key`，AI/导入创建接口以用户和键去重。
- 用户删除 AI 凭据后，引用该凭据的知识树保留但默认凭据置空；后续叶子节点追问要求用户选择新的已验证凭据。

## 6. 缓存、限流与任务

- Redis：Sa-Token 会话、热点文章、验证码、短期幂等结果、SSE 连接元数据和分布式锁。
- Sentinel：登录、AI 创建、文件上传凭证、公开文章查询分别配置限流与降级；AI 路由必须设置按用户的额度限制。
- XXL-JOB：定时发布文章、Outbox 补投、孤儿上传清理、过期 AI 任务清理、MinerU ZIP 解析失败重试、每日练习统计汇总。
- RocketMQ：`ai.task.created`、`question.mineru.import.requested`、`question.import.completed`、`learning.tree.generated`、`practice.recorded`。所有消费者幂等。节点扩展相关事件在首期不投递，保留给后续功能启用。

## 7. 可观测性与交付

- 所有 HTTP、Dubbo、RocketMQ、XXL-JOB 和 AI 调用携带 `traceId`。
- 记录延迟、错误率、Token 使用量、导入成功率、SSE 在线数、消息积压和任务重试次数。
- 配置按 `local`、`dev`、`prod` 放入 Nacos namespace；数据库、Redis、MQ、MinIO 和模型密钥只由环境变量注入。
- CI 至少执行编译、单元测试、API 契约测试、数据库迁移校验和依赖漏洞扫描。MySQL schema 由 Flyway 管理，禁止生产手工改表。
- 首期生产部署为单台服务器 Docker Compose，容器化运行 Nacos、MySQL、Redis、MinIO、RocketMQ、XXL-JOB、Gateway 和业务服务；部署入口提供环境变量模板与健康检查。
- MySQL 执行每日逻辑备份，MinIO 执行每日增量备份，二者均保留 7 天；MinerU 原始 ZIP 由 XXL-JOB 在成功导入 15 天后清理，已被题目引用的 Markdown 与图片不受影响。

## 8. 当前技术风险

| 风险 | 处理 |
| --- | --- |
| AI 输出不稳定 | 严格 Prompt、结构校验、失败可重试、原文留档。 |
| MinerU 导入包质量 | 不在本站集成 OCR；仅接收最大 30 MiB 的 MinerU ZIP，要求其中恰有一个 Markdown。导入器校验压缩包目录穿越、解压总量、文件数和图片 MIME，再将 Markdown 与图片分别安全存储。 |
| QQ 邮箱发送额度 | 个人邮箱可能触发频率或日发送量限制；验证码接口按邮箱/IP 限流并记录失败原因，部署时预留切换 SMTP 提供商的配置能力。 |
| 个人项目微服务运维复杂度 | Docker Compose 提供本地一键依赖；业务服务支持按需启动。 |
| 树图性能 | 初始限制每棵树 200 节点、深度 8；超限采用按层懒加载。 |
| 数据一致性 | 服务私有数据、Outbox、消费者去重与可观测补偿。 |

## 9. 资料依据

Spring Cloud Alibaba 当前兼容矩阵显示 `2025.0.x` 对应 Spring Boot `3.5.x`、Java 17+；Nacos 负责注册/配置、Gateway 负责网关、Sentinel 负责限流熔断。Sa-Token 的 Gateway 模式采用 `SaReactorFilter`，下游可验证 Same-Token。AgentScope Java 的生产文档建议共享 Agent 实例并用 `RuntimeContext` 按用户和会话隔离状态。
