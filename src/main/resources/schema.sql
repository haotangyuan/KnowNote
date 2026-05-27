-- KnowNote 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱',
    `username` VARCHAR(13) UNIQUE COMMENT '用户ID，最长13字符',
    `nickname` VARCHAR(64) COMMENT '昵称',
    `password_hash` VARCHAR(255) COMMENT '密码哈希（BCrypt）',
    `google_id` VARCHAR(255) UNIQUE COMMENT 'Google OpenID',
    `avatar` VARCHAR(512) COMMENT '头像URL',
    `bio` VARCHAR(200) COMMENT '个人简介',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- KnowNote 笔记表
CREATE TABLE IF NOT EXISTS `post` (
    `id` BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `creator_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者ID',
    `status` VARCHAR(20) NOT NULL COMMENT '状态（DRAFT, REVIEWING, PUBLISHED, DELETED, REJECTED）',
    `type` VARCHAR(20) NOT NULL COMMENT '类型（article）',
    `title` VARCHAR(255) COMMENT '标题',
    `description` VARCHAR(512) COMMENT '描述',
    `tags` JSON COMMENT '标签，逗号分隔',
    `cover_url` VARCHAR(512) COMMENT '封面URL',
    `img_urls` JSON COMMENT '图片URL列表',
    `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
    `content_url` VARCHAR(512) COMMENT '内容存储URL',
    `content_sha256` CHAR(64) COMMENT '内容SHA-256哈希',
    `published_version` VARCHAR(64) DEFAULT NULL COMMENT '已发布版本号',
    `reject_reason` VARCHAR(512) COMMENT '拒绝原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `published_at` DATETIME COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 研究会话表
CREATE TABLE IF NOT EXISTS research_session (
    id                  CHAR(32)        NOT NULL PRIMARY KEY COMMENT '研究ID (UUID)',
    user_id             BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    status              VARCHAR(32)     NOT NULL DEFAULT 'NEW' COMMENT '状态: NEW/QUEUE/START/IN_SCOPE/NEED_CLARIFICATION/IN_RESEARCH/IN_REPORT/COMPLETED/FAILED',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    start_time          DATETIME        DEFAULT NULL COMMENT '开始研究时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    complete_time       DATETIME        DEFAULT NULL COMMENT '完成时间',
    model_id            VARCHAR(256)    DEFAULT NULL COMMENT '模型ID',
    budget              VARCHAR(16)     DEFAULT NULL COMMENT '研究预算级别: MEDIUM/HIGH/ULTRA',
    title               VARCHAR(256)    DEFAULT NULL COMMENT '研究标题',
    total_input_tokens  BIGINT UNSIGNED DEFAULT 0 COMMENT '累计输入Token数',
    total_output_tokens BIGINT UNSIGNED DEFAULT 0 COMMENT '累计输出Token数',
    KEY idx_user_status (user_id, status),
    KEY idx_user_update (user_id, update_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究会话';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    research_id     CHAR(32)        NOT NULL COMMENT '研究ID',
    role            VARCHAR(16)     NOT NULL COMMENT '角色: user/assistant',
    content         MEDIUMTEXT      NOT NULL COMMENT '消息内容',
    sequence_no     INT UNSIGNED    NOT NULL COMMENT '序列号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_research_seq (research_id, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息';

-- 工作流事件表
CREATE TABLE IF NOT EXISTS workflow_event (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '事件ID',
    research_id     CHAR(32)        NOT NULL COMMENT '研究ID',
    type            VARCHAR(32)     NOT NULL COMMENT '事件类型',
    title           VARCHAR(512)    NOT NULL COMMENT '事件标题',
    content         MEDIUMTEXT      DEFAULT NULL COMMENT '事件内容',
    parent_event_id BIGINT UNSIGNED DEFAULT NULL COMMENT '父事件ID (用于层级缩进)',
    sequence_no     INT UNSIGNED    NOT NULL COMMENT '序列号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_research_seq (research_id, sequence_no),
    KEY idx_parent (parent_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流事件';

-- Studio 项目表
CREATE TABLE IF NOT EXISTS `studio_project` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    `name`        VARCHAR(255)    NOT NULL COMMENT '项目名称',
    `description` VARCHAR(512)    DEFAULT NULL COMMENT '项目描述',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'INIT' COMMENT '状态: INIT/ACTIVE/DELETED',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Studio项目表';
