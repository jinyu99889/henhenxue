# 狠狠学数据模型与表设计

## 1. 总体约定

- MySQL 8.0，`utf8mb4`，InnoDB，主键使用 `CHAR(26)` ULID；所有时间使用 `DATETIME(3)` UTC。
- 业务表统一有 `id`、`created_at`、`updated_at`、`created_by`、`updated_by`、`deleted_at`。软删仅用于用户可恢复或需审计的数据；关联唯一索引需要含 `deleted_at` 的替代设计或使用活动状态字段。
- 枚举存 `VARCHAR(32)`，不使用 MySQL `ENUM`，以便服务契约演进；金额/Token 计数等数值不使用浮点。
- `JSON` 仅用于可变结构（选项、答案、解析元数据）；可筛选和关联字段必须独立成列或关联表。
- 每个服务拥有自己的表；下文以逻辑前缀标识归属，不要求物理分库。

## 2. 认证与平台表

| 表 | 核心字段 | 约束/索引 | 说明 |
| --- | --- | --- | --- |
| `sys_user` | `id, username, email, email_verified_at, nickname, avatar_file_id, status, last_login_at` | `uk_username`, `uk_email` | 用户主体；邮箱用于验证与重置密码。 |
| `sys_user_identity` | `id, user_id, provider, identifier, password_hash, verified_at` | `uk(provider, identifier)` | 第一期开启 `PASSWORD` 身份；密码只存哈希。邮箱验证码是 Redis 短期数据，不落表。 |
| `sys_role` | `id, code, name, status` | `uk_code` | `USER`、`ADMIN`。 |
| `sys_user_role` | `user_id, role_id` | `pk(user_id, role_id)` | 用户角色。 |
| `sys_permission` | `id, code, name, resource, action, status` | `uk_code` | 权限码，如 `content:article:publish`、`system:user:manage`。 |
| `sys_role_permission` | `role_id, permission_id` | `pk(role_id, permission_id)` | RBAC 的角色-权限授权关系。 |
| `sys_file_object` | `id, owner_user_id, bucket, object_key, original_name, content_type, size, sha256, purpose, status` | `uk(bucket, object_key)`, `idx(owner_user_id, purpose)` | MinIO 元数据，`purpose` 限定用途。 |
| `sys_ai_credential` | `id, owner_user_id, provider_code, model_name, encrypted_secret, key_fingerprint, key_version, status, verified_at, last_used_at` | `uk(owner_user_id, provider_code, model_name)`, `idx(owner_user_id, status)` | 用户模型配置；提供商仅 `DEEPSEEK`、`DASHSCOPE`，官方地址由服务端固定，密文绝不返回 API 或日志。 |
| `sys_ai_task` | `id, requester_user_id, credential_id, credential_fingerprint, business_type, business_id, status, provider_code, model, request_hash, input_json, output_file_id, error_code, attempt_count, max_attempts, next_retry_at, started_at, finished_at` | `idx(requester_user_id, status)`, `idx(business_type, business_id)` | AI 异步任务，只保存凭据引用和指纹快照，不存密钥；记录内部重试状态。 |
| `sys_outbox_event` | `id, aggregate_type, aggregate_id, topic, event_key, payload_json, status, retry_count, next_retry_at, published_at` | `uk(topic, event_key)`, `idx(status, next_retry_at)` | 本地事务事件与可靠投递。 |
| `sys_operation_log` | `id, user_id, service, action, target_type, target_id, request_id, ip, result, detail_json` | `idx(target_type, target_id)`, `idx(user_id, created_at)` | 关键审计。 |

## 3. 内容服务表

| 表 | 核心字段 | 约束/索引 | 说明 |
| --- | --- | --- | --- |
| `content_article` | `id, author_user_id, article_type, slug, title, summary, cover_file_id, status, current_revision_id, published_at, scheduled_at, view_count, favorite_count` | `uk_slug`, `idx(article_type, status, published_at)`, `idx(author_user_id, status)` | 内容聚合根；`ARTICLE` 进入公开文章列表，`PAGE` 用于“关于我”等静态页。 |
| `content_article_revision` | `id, article_id, revision_no, title, summary, content_md, content_html, toc_json, change_note, created_by` | `uk(article_id, revision_no)`, `ft(title, summary, content_md) WITH PARSER ngram` | 发布内容和编辑历史；FULLTEXT 仅查询当前已发布版本，MySQL 配置 `ngram_token_size=2`。 |
| `content_article_revision_asset` | `revision_id, file_id, usage, sort_order` | `pk(revision_id, file_id, usage)`, `idx(file_id)` | 文章版本正文图片/附件关联；对象字节在 MinIO，元数据在 `sys_file_object`，回滚版本时资源引用保持一致。 |
| `content_category` | `id, name, slug, description, sort_order, status` | `uk_slug` | 分类。 |
| `content_tag` | `id, name, slug, status` | `uk_slug` | 标签。 |
| `content_article_category` | `article_id, category_id` | `pk(article_id, category_id)` | 一文多分类，若只保留主分类可加 `is_primary`。 |
| `content_article_tag` | `article_id, tag_id` | `pk(article_id, tag_id)` | 文章标签。 |
| `content_article_favorite` | `article_id, user_id, created_at` | `pk(article_id, user_id)` | 文章收藏，计数异步汇总。 |

## 4. 知识树服务表

树是版本化聚合。`node_key` 是跨版本稳定的逻辑标识，不能使用某一次快照的物理行 ID 做追问定位。

| 表 | 核心字段 | 约束/索引 | 说明 |
| --- | --- | --- | --- |
| `learn_tree` | `id, owner_user_id, default_credential_id, title, original_question, language, current_version_id, root_tree_id, source_node_key, status` | `idx(owner_user_id, updated_at)`, `idx(root_tree_id)` | 一棵学习树；保存首次生成和后续叶子追问的默认模型凭据，未来从节点建新树可用 `root_tree_id/source_node_key` 溯源。 |
| `learn_tree_version` | `id, tree_id, version_no, parent_version_id, generation_type, source_node_key, source_task_id, markdown_file_id, checksum, node_count, max_depth, status` | `uk(tree_id, version_no)`, `idx(tree_id, created_at)` | 不可变快照；`generation_type` 如 `INITIAL`、`EXPAND`、`MANUAL_EDIT`。 |
| `learn_node` | `id, tree_version_id, node_key, parent_node_key, depth, sort_order, title, content_md, summary, source_type, source_message_id` | `uk(tree_version_id, node_key)`, `idx(tree_version_id, parent_node_key, sort_order)`, `idx(tree_version_id, depth)` | 一个版本内的树节点。`parent_node_key` 为 null 的唯一根由应用校验。`content_md` 仅保存该标题直属正文，不重复保存任何子标题或后代正文。 |
| `learn_conversation` | `id, tree_id, owner_user_id, base_version_id, node_key, title, status` | `idx(tree_id, node_key, created_at)`, `idx(owner_user_id, updated_at)` | 围绕指定版本中叶子节点的追问会话。 |
| `learn_message` | `id, conversation_id, seq_no, role, content_md, context_json, ai_task_id, token_in, token_out, status` | `uk(conversation_id, seq_no)`, `idx(ai_task_id)` | 用户和 AI 消息；`context_json` 记录当次裁剪后的上下文来源。 |

关键完整性规则：

1. `learn_tree.current_version_id` 必须属于该树且状态为 `READY`。
2. 一个 `learn_tree_version` 恰有一个根节点，所有 `parent_node_key` 均在同版本存在。
3. 每个节点的直属正文不包含任何后代内容。根和非叶子节点的展示 Markdown 由服务端递归读取有序后代得到，不落冗余列。
4. `learn_conversation.base_version_id` 和 `node_key` 必须能定位该版本中的叶子节点；树版本回退不删除对话，也不得将旧版本会话展示到其他版本的同名节点。
5. 未来扩展版本复制原版本节点，保留节点 `node_key`；新增节点的父节点必须是用户指定的 `source_node_key`。

## 5. 题库与练习服务表

| 表 | 核心字段 | 约束/索引 | 说明 |
| --- | --- | --- | --- |
| `question_library` | `id, owner_user_id, library_type, name, description, status, question_count` | `uk(owner_user_id, name)`, `idx(owner_user_id, library_type, updated_at)` | 用户私有题库；`NORMAL` 为普通题库，`WRONG_QUESTION` 为系统创建且不可重命名的唯一错题知识库。 |
| `question_import` | `id, library_id, credential_id, source_archive_file_id, markdown_file_id, parser_type, status, total_count, success_count, failed_count, error_file_id, task_id, started_at, finished_at` | `idx(library_id, created_at)`, `uk(source_archive_file_id)` | 一次 MinerU ZIP 导入，显式记录用户选用的模型，保留原 ZIP、提取后的 Markdown 和错误报告。 |
| `question_import_asset` | `id, import_id, relative_path, file_id, content_hash` | `uk(import_id, relative_path)`, `uk(import_id, file_id)` | MinerU 包内图片的路径与 MinIO 文件元数据映射。 |
| `question_item` | `id, owner_user_id, import_id, type, stem_md, answer_json, analysis_md, difficulty, status, source_locator, content_hash, version_no` | `idx(owner_user_id, status, type)`, `idx(import_id)`, `idx(owner_user_id, content_hash)` | 用户的规范题目实体，可同时关联普通题库和错题知识库；内容哈希只用于排查，不限制重复题。 |
| `question_library_item` | `library_id, question_id, source_type, added_at` | `pk(library_id, question_id)`, `idx(question_id)` | 题库-题目关系；`source_type` 为 `MANUAL`、`MINERU_IMPORT` 或 `WRONG_ANSWER`。 |
| `question_item_asset` | `question_id, file_id, usage, sort_order` | `pk(question_id, file_id, usage)`, `idx(file_id)` | 题目正文、选项或解析引用的图片；图片字节在 MinIO，元数据在 `sys_file_object`。 |
| `question_option` | `id, question_id, option_key, content_md, sort_order` | `uk(question_id, option_key)` | 仅选择题需要。 |
| `question_tag` | `id, owner_user_id, name` | `uk(owner_user_id, name)` | 用户自定义知识点标签。 |
| `question_item_tag` | `question_id, tag_id` | `pk(question_id, tag_id)` | 题目-标签。 |
| `practice_session` | `id, user_id, scope, mode, question_count, order_mode, status, started_at, completed_at, config_json` | `idx(user_id, status, started_at)` | 一次固定练习；`scope` 为指定题库或创建时全部个人题库。 |
| `practice_session_library` | `session_id, library_id` | `pk(session_id, library_id)` | 会话选择的多个题库，包含可选的错题知识库。 |
| `practice_question_snapshot` | `id, session_id, question_id, sequence_no, question_json, answer_json, analysis_md, type, source_version_no` | `uk(session_id, sequence_no)`, `idx(session_id, question_id)` | 创建会话时冻结题目，答案只供后端返回。 |
| `practice_answer` | `id, session_id, snapshot_id, submitted_answer_json, result, self_assessment, duration_ms, submitted_at` | `uk(session_id, snapshot_id)`, `idx(session_id, result)` | 用户一次会话对一题只有一次最终答案。 |
| `practice_mastery` | `user_id, question_id, mastery_level, correct_count, wrong_count, last_practiced_at, next_review_at` | `pk(user_id, question_id)`, `idx(user_id, next_review_at)` | 为错题本与间隔重复预留，不依赖题目版本。 |

答案 JSON 约定：单选/多选 `{ "keys": ["A"] }`，判断 `{ "value": true }`，填空 `{ "answers": ["...", "..."] }`，主观 `{ "selfAssessment": "KNOW" }`。`answer_json` 不得出现在作答模式的“下一题”响应中。判定为 `INCORRECT`、`DONT_KNOW` 或 `NOT_MASTERED` 时，在同一事务内懒创建用户的 `WRONG_QUESTION` 题库，并幂等写入 `question_library_item`。用户从错题库移除题目时仅删除这条关联，不删除题目实体和普通题库关联。

## 6. 建表与迁移顺序

1. `sys_user`、身份、角色、文件、Outbox、审计。
2. 内容服务全部表。
3. 知识树、版本、节点、会话、消息。
4. 题库、导入、题目、选项、标签、练习、掌握度。

以服务为单位维护 Flyway 迁移，例如 `learning-service/src/main/resources/db/migration/V1__learning_baseline.sql`。上线后只追加迁移文件，不修改已执行的 V1。

## 7. 已确认细节

1. 文章支持多分类，使用 `content_article_category` 关联表。
2. 文章第一期只支持收藏，不实现评论、点赞或其他互动类型。
3. 知识树节点允许移动/排序，所有调整均生成新版本。
4. MinerU ZIP 的 Markdown 图片引用会在导入时改写为 `asset://{fileId}`，API 根据当前用户权限生成短时 MinIO 下载 URL；禁止在 Markdown 内直接保留用户上传的外部图片 URL。
5. DeepSeek 与 DashScope 的模型名不在前端硬编码。后端以用户输入的模型名发起校验，便于提供商新增模型时无需发布前端。
