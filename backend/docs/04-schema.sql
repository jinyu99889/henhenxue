-- 狠狠学一期 MySQL 8.0 建库脚本。
-- 所有外部 ID 为逻辑引用，故意不建跨服务外键；字段 COMMENT 即字段说明。
CREATE DATABASE IF NOT EXISTS hengxue CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE hengxue;

CREATE TABLE sys_user (
  id CHAR(26) NOT NULL COMMENT '用户 ULID',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名',
  email VARCHAR(254) NOT NULL COMMENT '验证邮箱',
  email_verified_at DATETIME(3) NULL COMMENT '邮箱验证时间',
  nickname VARCHAR(64) NOT NULL COMMENT '显示昵称',
  avatar_file_id CHAR(26) NULL COMMENT '头像文件 ID',
  preferences_json JSON NULL COMMENT '白名单用户偏好',
  status VARCHAR(32) NOT NULL COMMENT '用户状态',
  last_login_at DATETIME(3) NULL COMMENT '最后登录时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  created_by CHAR(26) NULL COMMENT '创建人',
  updated_by CHAR(26) NULL COMMENT '更新人',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  active_username VARCHAR(64) GENERATED ALWAYS AS (IF(deleted_at IS NULL, username, NULL)) STORED COMMENT '活跃用户名唯一键',
  active_email VARCHAR(254) GENERATED ALWAYS AS (IF(deleted_at IS NULL, email, NULL)) STORED COMMENT '活跃邮箱唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_username (active_username),
  UNIQUE KEY uk_active_email (active_email)
) ENGINE = InnoDB COMMENT = 'auth-service：用户主体';

CREATE TABLE sys_user_identity (
  id CHAR(26) NOT NULL COMMENT '身份 ULID',
  user_id CHAR(26) NOT NULL COMMENT '所属用户 ID',
  provider VARCHAR(32) NOT NULL COMMENT '身份提供方，首期 PASSWORD',
  identifier VARCHAR(254) NOT NULL COMMENT '登录标识',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  verified_at DATETIME(3) NULL COMMENT '验证时间',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  active_identifier VARCHAR(254) GENERATED ALWAYS AS (IF(deleted_at IS NULL, identifier, NULL)) STORED COMMENT '活跃登录标识唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_identifier (provider, active_identifier),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = 'auth-service：用户登录身份';

CREATE TABLE sys_role (
  id CHAR(26) NOT NULL COMMENT '角色 ID',
  code VARCHAR(64) NOT NULL COMMENT '角色码',
  name VARCHAR(64) NOT NULL COMMENT '角色名',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = 'auth-service：角色';

CREATE TABLE sys_permission (
  id CHAR(26) NOT NULL COMMENT '权限 ID',
  code VARCHAR(128) NOT NULL COMMENT '权限码',
  name VARCHAR(128) NOT NULL COMMENT '权限名',
  resource VARCHAR(64) NOT NULL COMMENT '资源',
  action VARCHAR(64) NOT NULL COMMENT '动作',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = 'auth-service：权限';

CREATE TABLE sys_user_role (
  user_id CHAR(26) NOT NULL COMMENT '用户 ID',
  role_id CHAR(26) NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB COMMENT = 'auth-service：用户角色关联';

CREATE TABLE sys_role_permission (
  role_id CHAR(26) NOT NULL COMMENT '角色 ID',
  permission_id CHAR(26) NOT NULL COMMENT '权限 ID',
  PRIMARY KEY (role_id, permission_id)
) ENGINE = InnoDB COMMENT = 'auth-service：角色权限关联';

CREATE TABLE sys_file_object (
  id CHAR(26) NOT NULL COMMENT '文件 ULID',
  owner_user_id CHAR(26) NOT NULL COMMENT '文件所有者',
  bucket VARCHAR(64) NOT NULL COMMENT 'MinIO 桶',
  object_key VARCHAR(512) NOT NULL COMMENT '对象键',
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  content_type VARCHAR(128) NOT NULL COMMENT 'MIME 类型',
  size BIGINT UNSIGNED NOT NULL COMMENT '字节数',
  sha256 CHAR(64) NOT NULL COMMENT '内容哈希',
  purpose VARCHAR(64) NOT NULL COMMENT '业务用途',
  status VARCHAR(32) NOT NULL COMMENT '上传状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bucket_object (bucket, object_key),
  KEY idx_owner_purpose (owner_user_id, purpose)
) ENGINE = InnoDB COMMENT = 'file-service：对象存储元数据';

CREATE TABLE sys_ai_credential (
  id CHAR(26) NOT NULL COMMENT '凭据 ULID',
  owner_user_id CHAR(26) NOT NULL COMMENT '所有者',
  provider_code VARCHAR(32) NOT NULL COMMENT '提供商',
  model_name VARCHAR(128) NOT NULL COMMENT '模型名',
  encrypted_secret VARBINARY(2048) NOT NULL COMMENT '信封加密密文',
  key_fingerprint CHAR(64) NOT NULL COMMENT '密钥指纹',
  key_version VARCHAR(64) NOT NULL COMMENT '主密钥版本',
  status VARCHAR(32) NOT NULL COMMENT '校验状态',
  verified_at DATETIME(3) NULL COMMENT '验证时间',
  last_used_at DATETIME(3) NULL COMMENT '最后使用时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  active_marker VARCHAR(26) GENERATED ALWAYS AS (
    IF(
      status <> 'REVOKED'
      AND deleted_at IS NULL,
      'ACTIVE',
      id
    )
  ) STORED COMMENT '活跃凭据唯一标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_credential (
    owner_user_id,
    provider_code,
    model_name,
    active_marker
  ),
  KEY idx_owner_status (owner_user_id, status)
) ENGINE = InnoDB COMMENT = 'ai-service：加密模型凭据';

CREATE TABLE sys_async_task (
  id CHAR(26) NOT NULL COMMENT '任务 ULID',
  requester_user_id CHAR(26) NOT NULL COMMENT '发起用户',
  task_type VARCHAR(64) NOT NULL COMMENT '任务类型',
  source_service VARCHAR(32) NOT NULL COMMENT '资源归属服务',
  business_type VARCHAR(64) NOT NULL COMMENT '资源类型',
  business_id CHAR(26) NOT NULL COMMENT '资源 ID',
  command_version SMALLINT UNSIGNED NOT NULL COMMENT '命令版本',
  command_json JSON NOT NULL COMMENT '不可变受控执行命令',
  status VARCHAR(32) NOT NULL COMMENT '任务状态',
  progress TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '进度 0-100',
  credential_id CHAR(26) NULL COMMENT '凭据 ID',
  credential_fingerprint CHAR(64) NULL COMMENT '凭据指纹快照',
  provider_code VARCHAR(32) NULL COMMENT '提供商快照',
  model VARCHAR(128) NULL COMMENT '模型快照',
  request_hash CHAR(64) NOT NULL COMMENT '请求哈希',
  result_type VARCHAR(64) NULL COMMENT '结果类型',
  result_id CHAR(26) NULL COMMENT '结果资源 ID',
  output_file_id CHAR(26) NULL COMMENT '原始输出文件',
  error_code VARCHAR(64) NULL COMMENT '失败码',
  error_message VARCHAR(500) NULL COMMENT '脱敏失败信息',
  attempt_count SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已尝试次数',
  max_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '最大尝试次数',
  next_retry_at DATETIME(3) NULL COMMENT '下次重试时间',
  started_at DATETIME(3) NULL COMMENT '开始时间',
  finished_at DATETIME(3) NULL COMMENT '最终完成时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_requester_status (requester_user_id, status, created_at),
  KEY idx_business (source_service, business_type, business_id)
) ENGINE = InnoDB COMMENT = 'ai-service：统一异步任务平台';

CREATE TABLE sys_idempotency_record (
  id CHAR(26) NOT NULL COMMENT '记录 ID',
  owner_service VARCHAR(32) NOT NULL COMMENT '写入服务类型',
  requester_user_id CHAR(26) NOT NULL COMMENT '请求用户',
  route VARCHAR(255) NOT NULL COMMENT '路由',
  idempotency_key CHAR(36) NOT NULL COMMENT '幂等键',
  request_hash CHAR(64) NOT NULL COMMENT '请求哈希',
  status VARCHAR(32) NOT NULL COMMENT '执行状态',
  resource_type VARCHAR(64) NULL COMMENT '资源类型',
  resource_id CHAR(26) NULL COMMENT '资源 ID',
  response_status SMALLINT UNSIGNED NULL COMMENT '响应 HTTP 状态',
  response_json JSON NULL COMMENT '首次响应',
  expires_at DATETIME(3) NOT NULL COMMENT '过期时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_idempotency (
    owner_service,
    requester_user_id,
    route,
    idempotency_key
  ),
  KEY idx_idempotency_expiry (expires_at),
  KEY idx_idempotency_service (owner_service, status)
) ENGINE = InnoDB COMMENT = '平台：统一幂等记录';

CREATE TABLE sys_outbox_event (
  id CHAR(26) NOT NULL COMMENT '事件 ID',
  producer_service VARCHAR(32) NOT NULL COMMENT '生产服务类型',
  aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型',
  aggregate_id CHAR(26) NOT NULL COMMENT '聚合 ID',
  topic VARCHAR(128) NOT NULL COMMENT '消息主题',
  event_key VARCHAR(128) NOT NULL COMMENT '去重键',
  payload_json JSON NOT NULL COMMENT '消息载荷',
  status VARCHAR(32) NOT NULL COMMENT '投递状态',
  retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
  next_retry_at DATETIME(3) NULL COMMENT '下次投递时间',
  published_at DATETIME(3) NULL COMMENT '投递时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox (producer_service, topic, event_key),
  KEY idx_outbox_retry (status, next_retry_at),
  KEY idx_outbox_service (producer_service, status)
) ENGINE = InnoDB COMMENT = '平台：统一可靠消息 Outbox';

CREATE TABLE sys_operation_log (
  id CHAR(26) NOT NULL COMMENT '日志 ID',
  user_id CHAR(26) NULL COMMENT '操作者',
  service VARCHAR(32) NOT NULL COMMENT '服务名',
  action VARCHAR(64) NOT NULL COMMENT '操作',
  target_type VARCHAR(64) NOT NULL COMMENT '目标类型',
  target_id CHAR(26) NULL COMMENT '目标 ID',
  request_id VARCHAR(64) NOT NULL COMMENT '请求追踪 ID',
  ip VARCHAR(45) NULL COMMENT '客户端 IP',
  result VARCHAR(32) NOT NULL COMMENT '操作结果',
  detail_json JSON NULL COMMENT '脱敏详情',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_target (target_type, target_id),
  KEY idx_user_created (user_id, created_at)
) ENGINE = InnoDB COMMENT = '平台：关键操作审计';

CREATE TABLE content_article (
  id CHAR(26) NOT NULL COMMENT '文章 ID',
  author_user_id CHAR(26) NOT NULL COMMENT '作者',
  article_type VARCHAR(32) NOT NULL COMMENT 'ARTICLE 或 PAGE',
  slug VARCHAR(128) NOT NULL COMMENT 'URL 标识',
  title VARCHAR(128) NOT NULL COMMENT '标题',
  summary VARCHAR(500) NOT NULL COMMENT '摘要',
  cover_file_id CHAR(26) NULL COMMENT '封面文件',
  status VARCHAR(32) NOT NULL COMMENT '文章状态',
  current_revision_id CHAR(26) NULL COMMENT '当前版本',
  published_at DATETIME(3) NULL COMMENT '最近发布时间',
  scheduled_at DATETIME(3) NULL COMMENT '定时发布时间',
  view_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '阅读计数',
  favorite_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏计数',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  created_by CHAR(26) NOT NULL COMMENT '创建人',
  updated_by CHAR(26) NOT NULL COMMENT '更新人',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  active_slug VARCHAR(128) GENERATED ALWAYS AS (IF(deleted_at IS NULL, slug, NULL)) STORED COMMENT '活跃 slug 唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_slug (active_slug),
  KEY idx_article_public (article_type, status, published_at),
  KEY idx_article_schedule (status, scheduled_at),
  KEY idx_author_status (author_user_id, status)
) ENGINE = InnoDB COMMENT = 'content-service：文章聚合';

CREATE TABLE content_article_revision (
  id CHAR(26) NOT NULL COMMENT '版本 ID',
  article_id CHAR(26) NOT NULL COMMENT '文章 ID',
  revision_no INT UNSIGNED NOT NULL COMMENT '版本号',
  title VARCHAR(128) NOT NULL COMMENT '标题快照',
  summary VARCHAR(500) NOT NULL COMMENT '摘要快照',
  content_md MEDIUMTEXT NOT NULL COMMENT 'Markdown 正文',
  content_html MEDIUMTEXT NULL COMMENT '净化 HTML',
  toc_json JSON NULL COMMENT '目录',
  change_note VARCHAR(500) NULL COMMENT '变更说明',
  created_by CHAR(26) NOT NULL COMMENT '创建人',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_article_revision (article_id, revision_no),
  FULLTEXT KEY ft_article_revision (title, summary, content_md)
  WITH
    PARSER ngram
) ENGINE = InnoDB COMMENT = 'content-service：不可变文章版本';

CREATE TABLE content_article_revision_asset (
  revision_id CHAR(26) NOT NULL COMMENT '版本 ID',
  file_id CHAR(26) NOT NULL COMMENT '文件 ID',
  `usage` VARCHAR(32) NOT NULL COMMENT '引用用途',
  sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '顺序',
  PRIMARY KEY (revision_id, file_id, `usage`),
  KEY idx_revision_asset_file (file_id)
) ENGINE = InnoDB COMMENT = '文章版本文件关联';

CREATE TABLE content_category (
  id CHAR(26) NOT NULL COMMENT '分类 ID',
  name VARCHAR(128) NOT NULL COMMENT '分类名',
  slug VARCHAR(128) NOT NULL COMMENT 'URL 标识',
  description VARCHAR(500) NULL COMMENT '说明',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  active_slug VARCHAR(128) GENERATED ALWAYS AS (IF(deleted_at IS NULL, slug, NULL)) STORED COMMENT '活跃 slug',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_slug (active_slug)
) ENGINE = InnoDB COMMENT = 'content-service：文章分类';

CREATE TABLE content_tag (
  id CHAR(26) NOT NULL COMMENT '标签 ID',
  name VARCHAR(128) NOT NULL COMMENT '标签名',
  slug VARCHAR(128) NOT NULL COMMENT 'URL 标识',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  active_slug VARCHAR(128) GENERATED ALWAYS AS (IF(deleted_at IS NULL, slug, NULL)) STORED COMMENT '活跃 slug',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_slug (active_slug)
) ENGINE = InnoDB COMMENT = 'content-service：文章标签';

CREATE TABLE content_article_category (
  article_id CHAR(26) NOT NULL COMMENT '文章 ID',
  category_id CHAR(26) NOT NULL COMMENT '分类 ID',
  PRIMARY KEY (article_id, category_id)
) ENGINE = InnoDB COMMENT = '文章分类关联';

CREATE TABLE content_article_tag (
  article_id CHAR(26) NOT NULL COMMENT '文章 ID',
  tag_id CHAR(26) NOT NULL COMMENT '标签 ID',
  PRIMARY KEY (article_id, tag_id)
) ENGINE = InnoDB COMMENT = '文章标签关联';

CREATE TABLE content_article_favorite (
  article_id CHAR(26) NOT NULL COMMENT '文章 ID',
  user_id CHAR(26) NOT NULL COMMENT '收藏用户',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '收藏时间',
  PRIMARY KEY (article_id, user_id)
) ENGINE = InnoDB COMMENT = '文章收藏';

CREATE TABLE learn_tree (
  id CHAR(26) NOT NULL COMMENT '树 ID',
  owner_user_id CHAR(26) NOT NULL COMMENT '所有者',
  default_credential_id CHAR(26) NULL COMMENT '默认凭据',
  title VARCHAR(128) NOT NULL COMMENT '标题',
  original_question TEXT NOT NULL COMMENT '原始问题',
  language VARCHAR(16) NOT NULL COMMENT '语言',
  pending_task_id CHAR(26) NULL COMMENT '创建或失败任务',
  status VARCHAR(32) NOT NULL COMMENT 'PENDING READY FAILED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  created_by CHAR(26) NOT NULL COMMENT '创建人',
  updated_by CHAR(26) NOT NULL COMMENT '更新人',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  KEY idx_tree_owner (owner_user_id, updated_at),
  KEY idx_tree_task (pending_task_id)
) ENGINE = InnoDB COMMENT = 'learning-service：知识树';

CREATE TABLE learn_node (
  id CHAR(26) NOT NULL COMMENT '节点行 ID',
  tree_id CHAR(26) NOT NULL COMMENT '知识树',
  node_key CHAR(26) NOT NULL COMMENT '节点键',
  parent_node_key CHAR(26) NULL COMMENT '父节点键',
  depth TINYINT UNSIGNED NOT NULL COMMENT '深度',
  sort_order INT UNSIGNED NOT NULL COMMENT '同级排序',
  title VARCHAR(256) NOT NULL COMMENT '标题',
  content_md MEDIUMTEXT NOT NULL COMMENT '直属正文',
  summary VARCHAR(500) NULL COMMENT '摘要',
  source_type VARCHAR(32) NOT NULL COMMENT '来源类型',
  source_message_id CHAR(26) NULL COMMENT '来源消息',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tree_node (tree_id, node_key),
  KEY idx_node_parent (tree_id, parent_node_key, sort_order),
  KEY idx_node_depth (tree_id, depth)
) ENGINE = InnoDB COMMENT = 'learning-service：知识树节点';

CREATE TABLE learn_conversation (
  id CHAR(26) NOT NULL COMMENT '会话 ID',
  tree_id CHAR(26) NOT NULL COMMENT '树 ID',
  owner_user_id CHAR(26) NOT NULL COMMENT '所有者',
  node_key CHAR(26) NOT NULL COMMENT '叶子节点键',
  title VARCHAR(128) NULL COMMENT '会话标题',
  status VARCHAR(32) NOT NULL COMMENT '会话状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_conversation_node (tree_id, node_key, created_at),
  KEY idx_conversation_owner (owner_user_id, updated_at)
) ENGINE = InnoDB COMMENT = 'learning-service：节点追问会话';

CREATE TABLE learn_message (
  id CHAR(26) NOT NULL COMMENT '消息 ID',
  conversation_id CHAR(26) NOT NULL COMMENT '会话 ID',
  seq_no INT UNSIGNED NOT NULL COMMENT '会话序号',
  role VARCHAR(16) NOT NULL COMMENT 'USER 或 ASSISTANT',
  content_md MEDIUMTEXT NOT NULL COMMENT '消息内容',
  context_json JSON NULL COMMENT '裁剪上下文来源',
  async_task_id CHAR(26) NULL COMMENT '生成任务',
  token_in INT UNSIGNED NULL COMMENT '输入 Token',
  token_out INT UNSIGNED NULL COMMENT '输出 Token',
  status VARCHAR(32) NOT NULL COMMENT '消息状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_message_sequence (conversation_id, seq_no),
  KEY idx_message_task (async_task_id)
) ENGINE = InnoDB COMMENT = 'learning-service：会话消息';

CREATE TABLE question_library (
  id CHAR(26) NOT NULL COMMENT '题库 ID',
  owner_user_id CHAR(26) NOT NULL COMMENT '所有者',
  library_type VARCHAR(32) NOT NULL COMMENT 'NORMAL 或 WRONG_QUESTION',
  name VARCHAR(128) NOT NULL COMMENT '题库名',
  description VARCHAR(500) NULL COMMENT '说明',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  question_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '题目数',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  active_name VARCHAR(128) GENERATED ALWAYS AS (IF(deleted_at IS NULL, name, NULL)) STORED COMMENT '活跃名称',
  wrong_library_marker VARCHAR(26) GENERATED ALWAYS AS (
    IF(
      library_type = 'WRONG_QUESTION'
      AND deleted_at IS NULL,
      'WRONG',
      id
    )
  ) STORED COMMENT '每用户唯一错题库',
  PRIMARY KEY (id),
  UNIQUE KEY uk_library_name (owner_user_id, active_name),
  UNIQUE KEY uk_wrong_library (owner_user_id, wrong_library_marker),
  KEY idx_library_owner (owner_user_id, library_type, updated_at)
) ENGINE = InnoDB COMMENT = 'question-service：用户题库';

CREATE TABLE question_import (
  id CHAR(26) NOT NULL COMMENT '导入 ID',
  library_id CHAR(26) NOT NULL COMMENT '目标题库',
  credential_id CHAR(26) NOT NULL COMMENT '使用凭据',
  source_archive_file_id CHAR(26) NOT NULL COMMENT 'MinerU ZIP 文件',
  markdown_file_id CHAR(26) NULL COMMENT '提取 Markdown 文件',
  parser_type VARCHAR(32) NOT NULL COMMENT '解析器类型',
  status VARCHAR(32) NOT NULL COMMENT '导入状态',
  total_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总数',
  success_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '成功数',
  failed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失败数',
  error_file_id CHAR(26) NULL COMMENT '错误报告文件',
  task_id CHAR(26) NOT NULL COMMENT '异步任务',
  started_at DATETIME(3) NULL COMMENT '开始时间',
  finished_at DATETIME(3) NULL COMMENT '完成时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_import_archive (source_archive_file_id),
  KEY idx_import_library (library_id, created_at)
) ENGINE = InnoDB COMMENT = 'question-service：MinerU 导入';

CREATE TABLE question_import_asset (
  id CHAR(26) NOT NULL COMMENT '关联 ID',
  import_id CHAR(26) NOT NULL COMMENT '导入 ID',
  relative_path VARCHAR(512) NOT NULL COMMENT '包内相对路径',
  file_id CHAR(26) NOT NULL COMMENT '对象文件',
  content_hash CHAR(64) NOT NULL COMMENT '内容哈希',
  PRIMARY KEY (id),
  UNIQUE KEY uk_import_path (import_id, relative_path),
  UNIQUE KEY uk_import_file (import_id, file_id)
) ENGINE = InnoDB COMMENT = '导入图片关联';

CREATE TABLE question_item (
  id CHAR(26) NOT NULL COMMENT '题目 ID',
  owner_user_id CHAR(26) NOT NULL COMMENT '所有者',
  import_id CHAR(26) NULL COMMENT '导入来源',
  type VARCHAR(32) NOT NULL COMMENT '题型',
  stem_md MEDIUMTEXT NOT NULL COMMENT '题干',
  answer_json JSON NOT NULL COMMENT '标准答案',
  analysis_md MEDIUMTEXT NOT NULL COMMENT '解析',
  difficulty TINYINT UNSIGNED NOT NULL COMMENT '难度 1-5',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  source_locator VARCHAR(512) NULL COMMENT '源文件位置',
  content_hash CHAR(64) NOT NULL COMMENT '内容哈希',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  created_by CHAR(26) NOT NULL COMMENT '创建人',
  updated_by CHAR(26) NOT NULL COMMENT '更新人',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  KEY idx_question_owner (owner_user_id, status, type),
  KEY idx_question_import (import_id),
  KEY idx_question_hash (owner_user_id, content_hash)
) ENGINE = InnoDB COMMENT = 'question-service：规范题目';

CREATE TABLE question_option (
  id CHAR(26) NOT NULL COMMENT '选项 ID',
  question_id CHAR(26) NOT NULL COMMENT '题目 ID',
  option_key VARCHAR(3) NOT NULL COMMENT '选项键',
  content_md TEXT NOT NULL COMMENT '选项内容',
  sort_order INT UNSIGNED NOT NULL COMMENT '排序',
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_option (question_id, option_key)
) ENGINE = InnoDB COMMENT = '选择题选项';

CREATE TABLE question_library_item (
  library_id CHAR(26) NOT NULL COMMENT '题库 ID',
  question_id CHAR(26) NOT NULL COMMENT '题目 ID',
  source_type VARCHAR(32) NOT NULL COMMENT '加入来源',
  added_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间',
  PRIMARY KEY (library_id, question_id),
  KEY idx_library_item_question (question_id)
) ENGINE = InnoDB COMMENT = '题库题目关联';

CREATE TABLE question_item_asset (
  question_id CHAR(26) NOT NULL COMMENT '题目 ID',
  file_id CHAR(26) NOT NULL COMMENT '文件 ID',
  `usage` VARCHAR(32) NOT NULL COMMENT '引用位置',
  sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (question_id, file_id, `usage`),
  KEY idx_question_asset_file (file_id)
) ENGINE = InnoDB COMMENT = '题目文件关联';

CREATE TABLE question_tag (
  id CHAR(26) NOT NULL COMMENT '标签 ID',
  owner_user_id CHAR(26) NOT NULL COMMENT '所有者',
  name VARCHAR(64) NOT NULL COMMENT '知识点名',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删时间',
  active_name VARCHAR(64) GENERATED ALWAYS AS (IF(deleted_at IS NULL, name, NULL)) STORED COMMENT '活跃名称',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_name (owner_user_id, active_name)
) ENGINE = InnoDB COMMENT = '用户知识点标签';

CREATE TABLE question_item_tag (
  question_id CHAR(26) NOT NULL COMMENT '题目 ID',
  tag_id CHAR(26) NOT NULL COMMENT '标签 ID',
  PRIMARY KEY (question_id, tag_id)
) ENGINE = InnoDB COMMENT = '题目知识点关联';

CREATE TABLE practice_session (
  id CHAR(26) NOT NULL COMMENT '会话 ID',
  user_id CHAR(26) NOT NULL COMMENT '用户 ID',
  scope VARCHAR(32) NOT NULL COMMENT '题库范围',
  mode VARCHAR(32) NOT NULL COMMENT 'MEMORIZE 或 EXAM',
  question_count INT UNSIGNED NOT NULL COMMENT '题数',
  order_mode VARCHAR(32) NOT NULL COMMENT '出题顺序',
  status VARCHAR(32) NOT NULL COMMENT '会话状态',
  started_at DATETIME(3) NOT NULL COMMENT '开始时间',
  completed_at DATETIME(3) NULL COMMENT '完成时间',
  config_json JSON NOT NULL COMMENT '创建时配置快照',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_practice_user (user_id, status, started_at)
) ENGINE = InnoDB COMMENT = 'question-service：练习会话';

CREATE TABLE practice_session_library (
  session_id CHAR(26) NOT NULL COMMENT '会话 ID',
  library_id CHAR(26) NOT NULL COMMENT '题库 ID',
  PRIMARY KEY (session_id, library_id)
) ENGINE = InnoDB COMMENT = '练习选择题库';

CREATE TABLE practice_question_snapshot (
  id CHAR(26) NOT NULL COMMENT '快照 ID',
  session_id CHAR(26) NOT NULL COMMENT '会话 ID',
  question_id CHAR(26) NOT NULL COMMENT '原题 ID',
  sequence_no INT UNSIGNED NOT NULL COMMENT '出题序号',
  question_json JSON NOT NULL COMMENT '题目快照',
  answer_json JSON NOT NULL COMMENT '答案快照',
  analysis_md MEDIUMTEXT NOT NULL COMMENT '解析快照',
  type VARCHAR(32) NOT NULL COMMENT '题型快照',
  source_version_no INT UNSIGNED NOT NULL COMMENT '题目版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_snapshot_sequence (session_id, sequence_no),
  KEY idx_snapshot_question (session_id, question_id)
) ENGINE = InnoDB COMMENT = '练习题目快照';

CREATE TABLE practice_answer (
  id CHAR(26) NOT NULL COMMENT '作答 ID',
  session_id CHAR(26) NOT NULL COMMENT '会话 ID',
  snapshot_id CHAR(26) NOT NULL COMMENT '题目快照',
  submitted_answer_json JSON NOT NULL COMMENT '提交答案',
  result VARCHAR(32) NOT NULL COMMENT '判题结果',
  self_assessment VARCHAR(32) NULL COMMENT '主观自评',
  duration_ms INT UNSIGNED NULL COMMENT '作答耗时毫秒',
  submitted_at DATETIME(3) NOT NULL COMMENT '提交时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_snapshot (session_id, snapshot_id),
  KEY idx_answer_result (session_id, result)
) ENGINE = InnoDB COMMENT = '练习最终作答';

CREATE TABLE practice_mastery (
  user_id CHAR(26) NOT NULL COMMENT '用户 ID',
  question_id CHAR(26) NOT NULL COMMENT '题目 ID',
  mastery_level VARCHAR(32) NOT NULL COMMENT '掌握程度',
  correct_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '正确次数',
  wrong_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '错误次数',
  last_practiced_at DATETIME(3) NULL COMMENT '最近练习',
  next_review_at DATETIME(3) NULL COMMENT '下次复习',
  PRIMARY KEY (user_id, question_id),
  KEY idx_mastery_review (user_id, next_review_at)
) ENGINE = InnoDB COMMENT = '题目掌握度';
