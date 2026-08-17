# 狠狠学接口文档

## 1. 通用约定

- 外部基础路径：`/api/v1`；全部 JSON 使用 `application/json; charset=utf-8`。
- 认证：除公开接口外均携带 Sa-Token 请求头。令牌名称由网关统一配置，前端不得自行解析令牌权限。
- `traceId` 通过 `X-Trace-Id` 请求头在链路中传递；Gateway 或入口服务在缺失或非法时生成，服务响应须回写该请求头，响应体中的 `traceId` 与其一致。
- 成功响应：`{ "code": 0, "message": "OK", "data": {}, "traceId": "..." }`。
- 失败响应遵循同一包装，`code` 为业务码；HTTP 使用 400、401、403、404、409、422、429、500、503 等语义状态。
- 分页请求：`page` 从 1 开始，`pageSize` 最大 100；分页响应为 `{items, page, pageSize, total}`。
- 写操作的幂等要求见 1.2；要求携带 `Idempotency-Key` 的端点重复请求时返回首次创建的资源或任务。
- 时间均为 ISO 8601 UTC；Markdown 字段以 `...Md` 命名；枚举采用全大写下划线。

### 1.1 契约状态与字段规则

本文与同目录的 [`openapi.yaml`](openapi.yaml) 共同构成第一期 HTTP 的规范性契约；`openapi.yaml` 是机器校验和契约测试的输入，本文解释业务状态机与跨端行为。不得以 Controller 实现反向定义接口。新生成的 ID 均为 26 位十进制字符串，其数值由雪花算法产生；已存在的 26 位 ULID 保持兼容。时间均为带 `Z` 的 UTC ISO 8601；未声明可空的字段不得为 `null`。请求中未定义的字段一律拒绝；`v1` 响应只可增加可选字段，删除字段、收紧语义或改变字段类型必须通过新的 API 版本演进。

字符串字段的通用上限：名称、标题 128；slug 128 且仅小写字母、数字、连字符；摘要 500；普通 Markdown 100 KiB；AI 原始问题和追问 8 KiB；分页 `pageSize` 默认为 20、最大 100。所有列表响应均使用 `{ "items": [], "page": 1, "pageSize": 20, "total": 0 }`。

### 1.2 错误、幂等与并发

失败响应为 `{ "code": "ERROR_CODE", "message": "面向用户的提示", "data": null, "traceId": "..." }`。`message` 不得包含密钥、令牌、内部 SQL 或上游响应原文。以下错误码是跨服务稳定码：

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 字段缺失、类型、长度、枚举或跨字段校验失败。 |
| 401 / 403 | `UNAUTHENTICATED` / `FORBIDDEN` | 未登录或无资源权限。 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在或对当前用户不可见。 |
| 409 | `VERSION_CONFLICT` | `If-Match` 或版本号与当前资源不一致。 |
| 409 | `IDEMPOTENCY_KEY_REUSED` | 相同幂等键对应不同请求体。 |
| 409 | `INVALID_STATE_TRANSITION` | 当前状态不允许目标操作。 |
| 409 | `TREE_NOT_READY` | 知识树仍在生成或生成失败，尚无可读版本。 |
| 409 | `REQUEST_IN_PROGRESS` | 同一幂等请求仍在执行；响应可附带 `Retry-After`。 |
| 422 | `LEARNING_NODE_NOT_LEAF` | 非叶子节点发起追问。 |
| 422 | `IMPORT_ASSET_INVALID` | 导入包资源或路径校验失败。 |
| 429 | `RATE_LIMITED` | 被限流；可附带 `retryAfterSeconds`。 |
| 500 | `INTERNAL_ERROR` | 未预期的服务端错误；不返回内部异常详情。 |
| 503 | `SERVICE_UNAVAILABLE` | 服务或关键依赖暂不可用，可安全重试。 |

`Idempotency-Key` 为 UUID，以下操作必须携带：注册、密码重置、创建或更新/撤销 AI 凭据、创建文章草稿或题库、手工创建题目、题目发布/归档、创建知识树、发起追问、创建导入任务、创建练习、提交答案、完成练习、文章发布/下架、版本激活、文件完成登记和从题库移除题目。客户端重试必须复用同一键。服务端以方法、路由和去除空白后按键名字典序序列化的 JSON 请求体计算 SHA-256 `requestHash`；相同键和哈希返回第一次的 HTTP 状态与响应体，仍在执行则返回已创建的资源/任务和 `202`，无资源可返回时才使用 `409 REQUEST_IN_PROGRESS`；相同键但不同哈希返回 `409 IDEMPOTENCY_KEY_REUSED`。记录保留 7 天。收藏/取消收藏以关联表唯一键保证天然幂等，不要求该请求头。

所有会改写既有聚合根或改变其状态的端点，包含 `PATCH`、发布、下架、归档、版本激活、版本恢复和树凭据切换，须携带 `If-Match: "<version>"`；响应返回新的 `ETag`。创建后未再被编辑的资源版本为 `1`。一期不在请求体同时传递 `expectedVersion`。

### 1.3 异步任务与轮询

长任务统一返回 `TaskAccepted`：

```json
{
  "taskId": "01J...",
  "status": "PENDING",
  "resourceType": "LEARNING_TREE",
  "resourceId": "01J..."
}
```

`GET /tasks/{taskId}` 返回 `AsyncTask`：`taskId`、`taskType`、`status`（`PENDING|RUNNING|SUCCEEDED|FAILED|CANCELLED`）、`progress`（0-100）、`resourceType`、`resourceId`、`errorCode`、`errorMessage`、`startedAt`、`finishedAt`。仅 `SUCCEEDED` 表示源服务已在本地事务中写入结果，客户端此时才可按 `resourceType` 和 `resourceId` 重读业务资源；仅任务请求者和管理员可读取。

前端在创建任务后按固定间隔轮询 `GET /tasks/{taskId}`，遇到 `RUNNING` 可展示服务端返回的 `progress`，遇到 `SUCCEEDED` 查询结果资源，遇到 `FAILED` 展示 `errorCode` 和可理解的 `errorMessage`，遇到 `CANCELLED` 停止轮询并提示任务已取消。MQ ACK 只代表消费者接受了消息，不作为前端成功条件。

## 2. 身份与用户

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 公开 | 账号密码注册；请求须带注册邮箱验证码。 |
| POST | `/auth/login` | 公开 | 密码登录，返回登录态与用户摘要。 |
| POST | `/auth/email-codes` | 公开 | 发送注册校验或重置密码验证码；注册邮箱已存在时返回 `409 EMAIL_ALREADY_REGISTERED`。 |
| POST | `/auth/password-resets` | 公开 | 使用邮箱验证码重置密码。 |
| POST | `/auth/password-changes` | 登录 | 使用旧密码修改当前账号密码。 |
| POST | `/auth/logout` | 登录 | 注销当前设备。 |
| POST | `/auth/refresh` | 登录 | 刷新可续期登录态。 |
| GET | `/users/me` | 登录 | 当前用户资料和权限。 |
| PATCH | `/users/me` | 登录 | 修改昵称、头像、学习偏好。 |
| POST | `/ai-credentials` | 登录 | 新增并立即连接校验用户模型凭据；仅本次请求传明文 Key。 |
| GET | `/ai-credentials` | 登录 | 获取当前用户的脱敏凭据列表。 |
| PATCH | `/ai-credentials/{credentialId}` | 登录 | 修改模型或轮换 Key，并立即重新连接校验。 |
| DELETE | `/ai-credentials/{credentialId}` | 登录 | 撤销凭据；待执行任务以 `CREDENTIAL_REVOKED` 失败，已向模型发起的单次请求允许结束。 |

登录会话使用 Redis 持久化 Sa-Token，令牌名称为 `satoken`，客户端通过同名请求头传递。允许同一账号多设备并发登录，每个账号最多保留 3 个有效会话，超过上限时按 Sa-Token 的先进先出策略注销最早会话。登录失败按来源 IP 与账号摘要分别限流；账号不存在、账号被禁用、身份软删或密码错误统一返回 `401 UNAUTHENTICATED`，未知账号仍执行固定 BCrypt 哈希校验以降低时序枚举风险。

凭据创建/更新请求仅支持固定提供商：

```json
{
  "providerCode": "DEEPSEEK",
  "apiKey": "sk-...",
  "model": "deepseek-chat"
}
```

`providerCode` 取值为 `DEEPSEEK` 或 `DASHSCOPE`。成功返回 `201 Created` 和脱敏凭据；连通性校验失败返回 `422`、业务码 `AI_CREDENTIAL_CONNECTION_FAILED`，其中包含可展示的错误类别（如 `INVALID_KEY`、`MODEL_NOT_FOUND`、`PROVIDER_TIMEOUT`），不回显 Key。

## 3. 博客

公开：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/articles` | 公开文章列表；支持 `keyword`、`categorySlug`、`tagSlug`、`sort=RELEVANCE|PUBLISHED_AT`、`page`。 |
| GET | `/articles/{slug}` | 文章详情和目录。 |
| GET | `/pages/about` | “关于我”静态页。 |
| GET | `/categories` | 分类及文章数。 |
| GET | `/tags` | 标签及文章数。 |
| GET | `/archives` | 按年月归档。 |
| POST | `/articles/{articleId}/favorite` | 登录用户收藏文章，幂等。 |
| DELETE | `/articles/{articleId}/favorite` | 登录用户取消收藏，幂等。 |

管理端：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/admin/articles` | 创建草稿。 |
| PATCH | `/admin/articles/{articleId}` | 使用 `ArticleUpsertRequest` 新建一条不可变 revision 并更新 `current_revision_id`；仅 `DRAFT`、`OFFLINE` 状态允许，须携带 `If-Match`。 |
| POST | `/admin/articles/{articleId}/publish` | 使用 `ArticlePublishRequest` 立即或定时发布；须携带 `Idempotency-Key` 和 `If-Match`。 |
| POST | `/admin/articles/{articleId}/offline` | 将 `PUBLISHED` 或 `SCHEDULED` 转为 `OFFLINE`；后者取消定时发布。须携带 `Idempotency-Key` 和 `If-Match`。 |
| GET | `/admin/articles/{articleId}/revisions` | 获取版本列表。 |
| POST | `/admin/articles/{articleId}/revisions/{revisionId}/restore` | 从历史版本复制为新的不可变 revision；仅 `DRAFT`、`OFFLINE` 状态允许，须携带 `Idempotency-Key` 和 `If-Match`。 |
| POST | `/admin/categories` | 创建分类；须携带 `Idempotency-Key`。 |
| PATCH | `/admin/categories/{categoryId}` | 修改分类；须携带 `If-Match`。 |
| POST | `/admin/categories/{categoryId}/archive` | 停用分类；须携带 `Idempotency-Key` 和 `If-Match`。 |
| POST | `/admin/tags` | 创建标签；须携带 `Idempotency-Key`。 |
| PATCH | `/admin/tags/{tagId}` | 修改标签；须携带 `If-Match`。 |
| POST | `/admin/tags/{tagId}/archive` | 停用标签；须携带 `Idempotency-Key` 和 `If-Match`。 |

文章正文图片/附件使用 `purpose=ARTICLE_CONTENT_ASSET` 申请上传凭证；保存文章版本时，服务端校验 Markdown 中的 `asset://{fileId}` 均属于当前作者并写入版本资源关联。

## 4. 文件与异步任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/files/upload-credentials` | 申请 MinIO 预签名上传 URL；请求含 `purpose`、文件名、MIME、大小、SHA-256。 |
| POST | `/files/{fileId}/complete` | 上传完成后的校验登记。 |
| GET | `/files/{fileId}/download-url` | 取得短时下载 URL。 |
| GET | `/tasks/{taskId}` | 查询 AI/导入任务状态和最终资源 ID。 |

`POST /files/upload-credentials` 请求示例：

```json
{
  "purpose": "QUESTION_MINERU_ARCHIVE",
  "fileName": "java-basics-mineru.zip",
  "contentType": "application/zip",
  "size": 2843210,
  "sha256": "hex-digest"
}
```

## 5. 知识树学习

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/learning/trees` | 创建树与生成任务。 |
| GET | `/learning/trees` | 当前用户的树列表。 |
| GET | `/learning/trees/{treeId}` | 仅 `READY` 树返回节点结构；`PENDING` 树返回 `409 TREE_NOT_READY`。 |
| GET | `/learning/trees/{treeId}/nodes/{nodeKey}/content` | 读取节点的展示 Markdown；根/非叶子返回子树，叶子返回直属正文。 |
| PATCH | `/learning/trees/{treeId}/credential` | 切换此树后续 AI 操作的默认模型凭据。 |
| POST | `/learning/trees/{treeId}/follow-ups` | 仅叶子节点追问，不改树。 |
| GET | `/learning/trees/{treeId}/conversations` | 查询指定叶子节点的会话列表。 |
| GET | `/learning/conversations/{conversationId}/messages` | 会话消息。 |

创建知识树：

```json
{
  "title": "JVM 垃圾回收",
  "originalQuestion": "请从原理到常见算法解释 JVM 垃圾回收",
  "credentialId": "01J...",
  "language": "zh-CN"
}
```

返回 `202 Accepted`：

```json
{
  "code": 0,
  "data": {
  "taskId": "01J...",
  "resourceType": "LEARNING_TREE",
  "resourceId": "01J...",
    "status": "PENDING"
  }
}
```

节点追问：

```json
{
  "nodeKey": "01J...",
  "question": "G1 为什么要把堆分成多个 Region？"
}
```

服务端校验 `nodeKey` 属于该树且为叶子节点；根节点或非叶子节点返回 `422 LEARNING_NODE_NOT_LEAF`，并且不得创建会话或 AI 任务。成功时返回 `202 Accepted`，其中包含 `conversationId` 与 `taskId`。节点会话列表携带 `nodeKey` 查询参数；对根或非叶子节点同样返回 `LEARNING_NODE_NOT_LEAF`。

节点展示内容：

`GET /learning/trees/{treeId}/nodes/{nodeKey}/content` 返回：

```json
{
  "treeId": "01J...",
  "nodeKey": "01J...",
  "title": "为什么单线程还能高性能",
  "path": [
    {"nodeKey": "01J-root", "title": "Redis 单线程模型：如何做到高效与线程安全"},
    {"nodeKey": "01J...", "title": "为什么单线程还能高性能"}
  ],
  "isLeaf": false,
  "followUpAllowed": false,
  "displayScope": "SUBTREE",
  "displayMd": "## 为什么单线程还能高性能\\n..."
}
```

叶子节点的 `displayScope` 为 `DIRECT`，`displayMd` 仅包含其直属正文。根节点和非叶子节点的 `displayScope` 为 `SUBTREE`，`displayMd` 包含该节点和全部后代的有序内容。服务端负责聚合 Markdown，客户端不得根据标题自行重建子树。

树详情中的节点最小模型：

```json
{
  "treeId": "01J...",
  "rootNodeKey": "01J...",
  "nodes": [
    {
      "nodeKey": "01J...",
      "parentNodeKey": null,
      "depth": 1,
      "sortOrder": 10,
      "title": "JVM 垃圾回收",
      "directContentMd": "...",
      "childrenCount": 3,
      "isLeaf": false,
      "followUpAllowed": false
    }
  ]
}
```

## 6. 题库与题目

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/question-libraries` | 创建题库。 |
| GET | `/question-libraries` | 获取自己的题库。 |
| PATCH | `/question-libraries/{libraryId}` | 修改名称、描述、可见性。 |
| POST | `/question-libraries/{libraryId}/imports` | 从已完成上传的 MinerU ZIP 输出包创建解析任务，必须传 `credentialId`。 |
| GET | `/question-imports/{importId}` | 导入进度和候选题统计。 |
| POST | `/question-libraries/{libraryId}/questions` | 手工创建题目。 |
| GET | `/question-libraries/{libraryId}/questions` | 题目列表，支持状态和题型筛选。 |
| PATCH | `/questions/{questionId}` | 修改候选或已发布题目。 |
| POST | `/questions/{questionId}/publish` | 审核后发布。 |
| POST | `/questions/{questionId}/archive` | 归档，不进入新练习。 |

手工题目请求示例：

```json
{
  "type": "SINGLE_CHOICE",
  "stemMd": "Java 中 `HashMap` 的默认负载因子是？",
  "options": [
    {"key": "A", "contentMd": "0.5"},
    {"key": "B", "contentMd": "0.75"}
  ],
  "answer": {"keys": ["B"]},
  "analysisMd": "默认负载因子为 0.75。",
  "difficulty": 2,
  "knowledgeTags": ["Java 集合"]
}
```

题库导入请求只接受 `.zip` MinerU 输出包，最大 30 MiB。页面提供 [MinerU](https://mineru.net/) 外链，用户完成转换后上传包含一个 `.md` 和图片资源目录的 ZIP，并以 `{ "fileId": "01J...", "credentialId": "01J..." }` 创建解析任务。服务端拒绝原始 `.pdf`、`.docx`、图片、单独 `.md`、多 Markdown 文件、软链接和含目录穿越路径的压缩包。压缩包解压后最大 100 MiB、最多 500 个文件，单张图片最大 10 MiB；图片只允许经 MIME 嗅探确认的 PNG、JPEG、WebP 或 GIF。任一 Markdown 图片链接未映射到包内有效图片时，返回 `422 IMPORT_ASSET_INVALID`，整个包不导入。

## 7. 练习

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/practice-sessions` | 根据一个或多个题库创建固定题目集。 |
| GET | `/practice-sessions` | 会话历史。 |
| GET | `/practice-sessions/{sessionId}` | 会话摘要与进度。 |
| GET | `/practice-sessions/{sessionId}/next` | 取得下一题；作答模式不会返回答案。 |
| POST | `/practice-sessions/{sessionId}/answers` | 提交客观题答案或主观题掌握状态。 |
| DELETE | `/question-libraries/{libraryId}/questions/{questionId}` | 从个人题库移除题目；答题结果页用此接口移出错题知识库。 |
| POST | `/practice-sessions/{sessionId}/finish` | 完成会话，生成统计。 |
| GET | `/practice-statistics` | 掌握度、正确率、错题分布。 |

创建会话：

```json
{
  "scope": "SELECTED_LIBRARIES",
  "libraryIds": ["01J...", "01J..."],
  "mode": "EXAM",
  "questionTypes": ["SINGLE_CHOICE", "MULTIPLE_CHOICE", "JUDGE"],
  "questionCount": 30,
  "order": "RANDOM"
}
```

`scope` 取值为 `SELECTED_LIBRARIES` 或 `ALL_MY_LIBRARIES`。后者的 `libraryIds` 必须为空；服务端在创建会话时读取当时全部可用的个人题库并写入会话快照，不能在练习中动态扩大范围。

作答提交：

```json
{
  "questionSnapshotId": "01J...",
  "answer": {"keys": ["B"]}
}
```

对于 `ESSAY`，使用 `{ "selfAssessment": "KNOW" }` 或 `{ "selfAssessment": "DONT_KNOW" }`。服务端响应包含 `result`、`correctAnswer`、`analysisMd`；作答模式的“下一题”只返回题干、选项和作答所需字段，不返回答案或解析；背题模式首次取题即包含答案和解析。

答题结果响应还返回 `wrongLibrary`：`{ "libraryId": "01J...", "containsQuestion": true }`。当 `containsQuestion` 为 `true` 时，前端展示“移出错题库”，调用题库移除接口；该动作只删除错题库与题目的关联，不删除原题或其他题库中的关联。

## 8. 主资源请求与响应 Schema

以下字段模型为端点表中未展开 payload 的统一定义。所有对象均包在通用成功响应的 `data` 内；创建成功返回 `201`，接受异步任务返回 `202`，无响应体的幂等删除返回 `204`。

### 身份与用户

`EmailCodeRequest`：`email`（email，必填）、`purpose`（`REGISTER|PASSWORD_RESET`，必填）。`RegisterRequest`：`username`（8-64）、`email`、`emailCode`（6 位）、`password`（8-128，且必须同时包含字母和数字）、`nickname`（1-64，必填）。`LoginRequest`：`account`（用户名或邮箱）、`password`。`UserProfilePatchRequest`：`nickname`（1-64，可选）、`avatarFileId`（ULID，可选）、`preferences`（对象，可选；仅白名单学习偏好键）。`UserSummary`：`id`、`username`、`email`、`emailVerifiedAt`、`nickname`、`avatarFileId`、`permissions`、`version`。

`AiCredentialUpsertRequest`：`providerCode`（`DEEPSEEK|DASHSCOPE`）、`model`（1-128）、`apiKey`（1-512）。`AiCredential`：`id`、`providerCode`、`model`、`keyFingerprintSuffix`、`status`（`VERIFYING|VERIFIED|FAILED|REVOKED`）、`verifiedAt`、`lastUsedAt`、`version`；响应中绝不出现 `apiKey` 或密文。

### 文章与文件

`ArticleUpsertRequest`：`articleType`（`ARTICLE|PAGE`）、`slug`、`title`、`summary`、`coverFileId`（ULID 或 null）、`contentMd`、`categoryIds`（最多 5 个 ULID）、`tagIds`（最多 20 个 ULID）、`changeNote`（0-500）。每次成功编辑均创建新 revision。`ArticlePublishRequest`：可选 `scheduledAt`；省略即立即发布。`ArticleSummary`：`id`、`slug`、`title`、`summary`、`coverFileId`、`categories`、`tags`、`publishedAt`、`readingMinutes`、`favoriteCount`、`isFavorited`。`ArticleDetail` 在此基础上增加 `contentMd`、`toc`、`currentRevisionNo`；公开接口只返回 `PUBLISHED` 文章。

`CategoryUpsertRequest`：`name`、`slug`、可选 `description`、`sortOrder`、`status`（`ACTIVE|ARCHIVED`）。`TagUpsertRequest`：`name`、`slug`、`status`。创建、修改、停用的管理端响应返回 `CategoryAdmin` 或 `TagAdmin`；分类额外包含 `description`、`sortOrder`，两者都包含 `id`、`name`、`slug`、`status`、`version`。公开列表仅返回可见项及文章数。

`UploadCredentialRequest`：`purpose`、`fileName`、`contentType`、`size`、`sha256`。`UploadCredential`：`fileId`、`uploadUrl`、`requiredHeaders`、`expiresAt`。`FileObject`：`id`、`originalName`、`contentType`、`size`、`sha256`、`purpose`、`status`；下载接口只返回 `{ "url": "...", "expiresAt": "..." }`。

### 知识树

`CreateTreeRequest`：`title`、`originalQuestion`、`credentialId`、`language`；返回 `TaskAccepted`，其中 `resourceType=LEARNING_TREE`。`TreeSummary`：`id`、`title`、`language`、`status`、`pendingTaskId`、`updatedAt`；当 `status=PENDING` 或 `FAILED` 时，`pendingTaskId` 保留创建任务 ID 以便查询进度或失败原因；当 `status=READY` 时为 `null`。`GET /learning/trees/{treeId}` 对 `PENDING` 或 `FAILED` 返回 `409 TREE_NOT_READY`，由前端使用 `pendingTaskId` 轮询任务状态；`TreeDetail` 只用于 `READY` 树，字段为 `treeId`、`rootNodeKey`、`nodes`；节点字段为 `nodeKey`、`parentNodeKey`、`depth`、`sortOrder`、`title`、`directContentMd`、`childrenCount`、`isLeaf`、`followUpAllowed`。

`FollowUpRequest`：`nodeKey`、`question`；返回 `conversationId` 和 `TaskAccepted`。`ConversationMessage`：`id`、`seqNo`、`role`（`USER|ASSISTANT`）、`contentMd`、`status`、`createdAt`。会话和消息持久化保存，用户第二天仍可通过会话列表和消息接口查看原问答流；这不是树版本历史。

### 题库与练习

`QuestionLibraryCreateRequest`：`name`（1-128）、`description`（0-500）；`QuestionLibraryPatchRequest`：`name`、`description`。`QuestionLibrary`：`id`、`name`、`description`、`libraryType`（`NORMAL|WRONG_QUESTION`）、`status`、`questionCount`、`version`、`updatedAt`。`QuestionImportCreateRequest`：`fileId`、`credentialId`；返回 `TaskAccepted`，其中 `resourceType=QUESTION_IMPORT`。

`QuestionUpsertRequest` 按 `type` 采用互斥变体：`SINGLE_CHOICE` 的 `options` 至少 2 项、`answer.keys` 恰为 1 项；`MULTIPLE_CHOICE` 的 `options` 至少 2 项、`answer.keys` 至少 1 项；`JUDGE` 不得传 `options`，`answer.value` 为布尔值；`FILL_BLANK` 不得传 `options`，`answer.answers` 至少 1 项；`ESSAY` 不得传 `options`，`answer.selfAssessment` 为 `KNOW|DONT_KNOW`。选择题选项键唯一，且所有答案键必须属于 `options`。所有变体均包含 `stemMd`、`analysisMd`、`difficulty`（1-5）和 `knowledgeTags`。管理端使用 `QuestionAdmin`，包含 `answer`；作答模式使用 `PracticeQuestion`，不得包含 `answer`，提交后才返回正确答案和解析。

`PracticeSessionCreateRequest`：`scope`、`libraryIds`、`mode`、`questionTypes`、`questionCount`、`order`；`scope=ALL_MY_LIBRARIES` 时 `libraryIds` 必须为空。`PracticeAnswerRequest`：`questionSnapshotId`、`answer`；响应为 `result`、`correctAnswer`、`analysisMd`、`wrongLibrary`。`PracticeSession`：`id`、`mode`、`status`、`questionCount`、`answeredCount`、`correctCount`、`startedAt`、`completedAt`。
