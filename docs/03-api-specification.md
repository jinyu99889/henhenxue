# 狠狠学接口文档

## 1. 通用约定

- 外部基础路径：`/api/v1`；全部 JSON 使用 `application/json; charset=utf-8`。
- 认证：除公开接口外均携带 Sa-Token 请求头。令牌名称由网关统一配置，前端不得自行解析令牌权限。
- 成功响应：`{ "code": 0, "message": "OK", "data": {}, "traceId": "..." }`。
- 失败响应遵循同一包装，`code` 为业务码；HTTP 使用 400、401、403、404、409、422、429、500、503 等语义状态。
- 分页请求：`page` 从 1 开始，`pageSize` 最大 100；分页响应为 `{items, page, pageSize, total}`。
- 写操作可带 `Idempotency-Key`；重复请求返回首次创建的资源或任务。
- 时间均为 ISO 8601 UTC；Markdown 字段以 `...Md` 命名；枚举采用全大写下划线。

## 2. 身份与用户

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 公开 | 账号密码注册；请求须带注册邮箱验证码。 |
| POST | `/auth/login` | 公开 | 密码登录，返回登录态与用户摘要。 |
| POST | `/auth/email-codes` | 公开 | 发送注册校验或重置密码验证码。 |
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
| PATCH | `/admin/articles/{articleId}` | 更新文章元数据和 Markdown。 |
| POST | `/admin/articles/{articleId}/publish` | 立即或定时发布。 |
| POST | `/admin/articles/{articleId}/offline` | 下线。 |
| GET | `/admin/articles/{articleId}/revisions` | 获取版本列表。 |
| POST | `/admin/articles/{articleId}/revisions/{revisionId}/restore` | 从版本恢复为新草稿。 |

文章正文图片/附件使用 `purpose=ARTICLE_CONTENT_ASSET` 申请上传凭证；保存文章版本时，服务端校验 Markdown 中的 `asset://{fileId}` 均属于当前作者并写入版本资源关联。

## 4. 文件与异步任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/files/upload-credentials` | 申请 MinIO 预签名上传 URL；请求含 `purpose`、文件名、MIME、大小、SHA-256。 |
| POST | `/files/{fileId}/complete` | 上传完成后的校验登记。 |
| GET | `/files/{fileId}/download-url` | 取得短时下载 URL。 |
| GET | `/tasks/{taskId}` | 查询 AI/导入任务状态和最终资源 ID。 |
| GET | `/tasks/{taskId}/events` | SSE；推送 `progress`、`delta`、`completed`、`failed` 事件。 |

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
| GET | `/learning/trees/{treeId}` | 返回当前版本的节点结构；可传 `versionId` 查看历史。 |
| GET | `/learning/trees/{treeId}/nodes/{nodeKey}/content` | 读取指定版本节点的展示 Markdown；根/非叶子返回子树，叶子返回直属正文。 |
| GET | `/learning/trees/{treeId}/versions` | 版本列表和变更原因。 |
| POST | `/learning/trees/{treeId}/versions/{versionId}/activate` | 将历史版本设为当前版本。 |
| PATCH | `/learning/trees/{treeId}/credential` | 切换此树后续 AI 操作的默认模型凭据。 |
| PATCH | `/learning/trees/{treeId}/nodes/{nodeKey}` | 编辑节点，异步生成新版本。 |
| POST | `/learning/trees/{treeId}/follow-ups` | 仅叶子节点追问，不改树。 |
| GET | `/learning/trees/{treeId}/conversations` | 查询指定版本、指定叶子节点的会话列表。 |
| GET | `/learning/conversations/{conversationId}/messages` | 会话消息。 |
| POST | `/learning/trees/{treeId}/expansions` | 后续功能，首期关闭。 |

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
    "treeId": "01J...",
    "taskId": "01J...",
    "status": "PENDING"
  }
}
```

节点追问：

```json
{
  "versionId": "01J...",
  "nodeKey": "01J...",
  "question": "G1 为什么要把堆分成多个 Region？"
}
```

服务端使用 `versionId` 校验 `nodeKey` 属于该树版本且为叶子节点；根节点或非叶子节点返回 `422 LEARNING_NODE_NOT_LEAF`，并且不得创建会话或 AI 任务。成功时返回 `202 Accepted`，其中包含 `conversationId` 与 `taskId`。节点会话列表必须携带 `versionId` 和 `nodeKey` 查询参数；对根或非叶子节点同样返回 `LEARNING_NODE_NOT_LEAF`。

节点展示内容：

`GET /learning/trees/{treeId}/nodes/{nodeKey}/content?versionId=01J...` 返回：

```json
{
  "treeId": "01J...",
  "versionId": "01J...",
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

后续功能：扩展节点。首期接口返回 `404 FEATURE_NOT_ENABLED`，不创建任务。

```json
{
  "sourceVersionId": "01J...",
  "nodeKey": "01J...",
  "action": "DIRECT_CHILDREN",
  "instruction": "补充调优时最常见的三个问题"
}
```

功能启用后，`action` 取值为 `DIRECT_CHILDREN`、`NEW_TREE_FROM_NODE`。前者返回新树版本任务，后者返回新知识树任务。任务完成后前端通过 SSE 收到 `resourceType`、`treeId`、`versionId`。

树详情中的节点最小模型：

```json
{
  "treeId": "01J...",
  "currentVersionId": "01J...",
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

对于 `ESSAY`，使用 `{ "selfAssessment": "KNOW" }` 或 `{ "selfAssessment": "DONT_KNOW" }`。服务端响应包含 `result`、`correctAnswer`、`analysisMd`；背题模式首次取题即包含答案和解析。

答题结果响应还返回 `wrongLibrary`：`{ "libraryId": "01J...", "containsQuestion": true }`。当 `containsQuestion` 为 `true` 时，前端展示“移出错题库”，调用题库移除接口；该动作只删除错题库与题目的关联，不删除原题或其他题库中的关联。
