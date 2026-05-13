# ER 图与建表 SQL

> "青隅"校园互助服务平台数据库设计
> 基于 P2 ADR-002（MySQL 8.0 单一数据库）、P2 模块划分（8 大模块 18 张表）设计

---

## 一、ER 图（实体关系图）

### 1.1 核心实体关系总览

```
┌──────────────────────────────────────────────────────────────────────┐
│                        用户与认证实体关系                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐   1   1  ┌─────────────────┐                       │
│  │   tb_user   │──────────│ tb_privacy_setting│                     │
│  │             │  has     │                   │                     │
│  └──────┬──────┘          └───────────────────┘                     │
│         │ 1                                                         │
│         │                                                           │
│         │ 1                                                         │
│  ┌──────┴──────┐                                                     │
│  │tb_user_cert │   (实名认证记录)                                      │
│  └─────────────┘                                                     │
│         │                                                           │
│         │ 1   1  ┌─────────────────┐                                 │
│         └────────│tb_love_profile  │  (交友资料, 1:1)                │
│                  └─────────────────┘                                 │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                         树洞模块实体关系                                │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐   1   *  ┌───────────────┐   1   *  ┌──────────────┐  │
│  │ tb_user  │──────────│tb_treehole_post│──────────│tb_treehole_  │  │
│  │          │publishes │               │   has    │comment       │  │
│  └──────────┘          └──────┬────────┘          └──────────────┘  │
│                               │                                      │
│                               │ 1                                    │
│                               │                                      │
│                               │ *                                    │
│                      ┌────────┴────────┐                            │
│                      │tb_treehole_like │                            │
│                      └─────────────────┘                            │
│                                                                      │
│  说明：tb_user 1:N tb_treehole_post 1:N tb_treehole_comment          │
│        tb_treehole_post 1:N tb_treehole_like (M:N of user)          │
│        tb_treehole_comment 自关联 parent_id（支持回复）              │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                        找搭子模块实体关系                               │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐   1   *  ┌────────────────┐                           │
│  │ tb_user  │──────────│tb_partner_req  │  (发布搭子需求)            │
│  │          │publishes │                │                           │
│  └────┬─────┘          └───────┬────────┘                           │
│       │                        │ 1                                   │
│       │                        │                                     │
│       │ *                      │ *                                   │
│  ┌────┴──────────────┐  ┌──────┴──────────┐                        │
│  │tb_partner_match   │  │tb_partner_chat  │                        │
│  │ (applicant FK)    │  │(matchId FK)     │                        │
│  └────────┬──────────┘  └─────────────────┘                        │
│           │ 1                                                       │
│           │                                                         │
│           │ 2 (互评)                                                 │
│  ┌────────┴──────────┐                                              │
│  │tb_partner_review  │                                              │
│  └───────────────────┘                                              │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                       恋爱互助模块实体关系                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐   1   1  ┌────────────────┐   1   1  ┌──────────────┐ │
│  │ tb_user  │──────────│tb_love_profile │──────────│tb_love_req   │ │
│  │          │  has     │                │  posts   │              │ │
│  └────┬─────┘          └────────────────┘          └──────┬───────┘ │
│       │                                                   │ 1       │
│       │ *                                                 │ *       │
│  ┌────┴──────────────┐                          ┌─────────┴───────┐ │
│  │ tb_love_match     │                          │  tb_love_chat   │ │
│  │ (applicant FK到   │                          │  (matchId FK)   │ │
│  │  tb_user)         │                          └─────────────────┘ │
│  └────────┬──────────┘                                              │
│           │ 1                                                       │
│           │ 2                                                       │
│  ┌────────┴──────────┐                                              │
│  │ tb_love_review    │                                              │
│  └───────────────────┘                                              │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                      消息通知 & 审核系统实体关系                        │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐   1   *  ┌─────────────────┐                          │
│  │ tb_user  │──────────│tb_notification  │  (收到通知)               │
│  └──────────┘          └─────────────────┘                          │
│                                                                      │
│  ┌──────────┐   1   *  ┌─────────────────┐                          │
│  │ tb_admin │──────────│tb_review_record │  (处理审核)               │
│  └──────────┘          └────────┬────────┘                          │
│                                 │                                    │
│                    审核对象为以下内容之一:                              │
│                    tb_treehole_post, tb_treehole_comment,             │
│                    tb_partner_req, tb_love_profile                   │
│                                                                      │
│  ┌──────────┐   1   *  ┌─────────────────┐                          │
│  │ tb_user  │──────────│ tb_feedback     │  (提交反馈)               │
│  └──────────┘          └─────────────────┘                          │
└──────────────────────────────────────────────────────────────────────┘
```

### 1.2 实体间关系汇总

| 关系 | 实体A | 实体B | 基数 | 说明 |
|------|-------|-------|------|------|
| 拥有一对一 | tb_user | tb_privacy_setting | 1:1 | 每个用户有一份隐私设置 |
| 拥有一对一 | tb_user | tb_user_cert | 1:1 | 每个用户有一条认证记录 |
| 拥有一对一 | tb_user | tb_love_profile | 1:1 | 每个用户有一份交友资料 |
| 发布一对多 | tb_user | tb_treehole_post | 1:N | 一个用户发布多条树洞动态 |
| 包含一对多 | tb_treehole_post | tb_treehole_comment | 1:N | 一条动态有多条评论 |
| 评论自关联 | tb_treehole_comment | tb_treehole_comment | 1:N | 评论支持回复（parent_id） |
| 点赞多对多 | tb_user | tb_treehole_post | M:N | 通过 tb_treehole_like 关联 |
| 发布一对多 | tb_user | tb_partner_req | 1:N | 一个用户发布多条搭子需求 |
| 申请多对多 | tb_user | tb_partner_req | M:N | 通过 tb_partner_match 关联 |
| 聊天一对多 | tb_partner_match | tb_partner_chat | 1:N | 一次匹配多条聊天 |
| 评价一对二 | tb_partner_match | tb_partner_review | 1:2 | 匹配双方互评 |
| 心动多对多 | tb_user | tb_love_req | M:N | 通过 tb_love_match 关联 |
| 接收一对多 | tb_user | tb_notification | 1:N | 一个用户多条通知 |
| 处理一对多 | tb_admin | tb_review_record | 1:N | 一个管理员处理多条审核 |
| 提交一对多 | tb_user | tb_feedback | 1:N | 一个用户提交多条反馈 |

---

## 二、建表 SQL

### 2.1 用户相关表

```sql
-- 用户表
CREATE TABLE tb_user (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(16)     NOT NULL,
    phone           VARCHAR(16)     NOT NULL COMMENT '手机号，AES加密存储',
    password_hash   VARCHAR(255)    NOT NULL COMMENT 'BCrypt哈希',
    avatar          VARCHAR(255)    DEFAULT NULL,
    account_status  VARCHAR(16)     NOT NULL DEFAULT 'NORMAL'
                    COMMENT 'NORMAL / WARNED / MUTED / BANNED',
    login_fail_cnt  TINYINT         NOT NULL DEFAULT 0,
    locked_until    DATETIME        DEFAULT NULL COMMENT '账号锁定截止时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        DEFAULT NULL COMMENT '注销时间(软删除)',

    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    INDEX idx_account_status (account_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户认证信息表（实名认证）
CREATE TABLE tb_user_cert (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    student_id      VARCHAR(20)     NOT NULL COMMENT '学号，加密存储',
    real_name       VARCHAR(32)     NOT NULL COMMENT '真实姓名，加密存储',
    id_card         VARCHAR(18)     NOT NULL COMMENT '身份证号，加密存储',
    university      VARCHAR(64)     NOT NULL,
    major           VARCHAR(64)     NOT NULL,
    grade           VARCHAR(16)     NOT NULL,
    gender          VARCHAR(8)      NOT NULL COMMENT 'male / female',
    age             TINYINT         NOT NULL,
    interests       VARCHAR(512)    DEFAULT NULL COMMENT '兴趣爱好，JSON数组',
    cert_status     VARCHAR(16)     NOT NULL DEFAULT 'UNCERTIFIED'
                    COMMENT 'UNCERTIFIED / PENDING / CERTIFIED / REJECTED',
    cert_attempts   TINYINT         NOT NULL DEFAULT 0,
    face_verified   TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_student_id (student_id),
    INDEX idx_cert_status (cert_status),
    CONSTRAINT fk_cert_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户实名认证信息表';

-- 隐私设置表
CREATE TABLE tb_privacy_setting (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    show_real_name  VARCHAR(16)     NOT NULL DEFAULT 'SELF_ONLY'
                    COMMENT 'ONLY_SELF / FRIENDS / ALL',
    show_student_id VARCHAR(16)     NOT NULL DEFAULT 'SELF_ONLY',
    show_university VARCHAR(16)     NOT NULL DEFAULT 'FRIENDS',
    show_major      VARCHAR(16)     NOT NULL DEFAULT 'FRIENDS',
    show_age        VARCHAR(16)     NOT NULL DEFAULT 'FRIENDS',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_privacy_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户隐私设置表';

-- 管理员表
CREATE TABLE tb_admin (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    admin_level     VARCHAR(16)     NOT NULL DEFAULT 'JUNIOR'
                    COMMENT 'SUPER / SENIOR / JUNIOR',
    permissions     VARCHAR(512)    NOT NULL DEFAULT '[]' COMMENT 'JSON 权限列表',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';
```

### 2.2 树洞模块表

```sql
-- 树洞动态表
CREATE TABLE tb_treehole_post (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    anonymous_name  VARCHAR(32)     NOT NULL COMMENT '系统自动生成的匿名昵称',
    content         VARCHAR(500)    NOT NULL,
    category        VARCHAR(16)     NOT NULL DEFAULT 'other'
                    COMMENT 'emotion / study / life / fun / other',
    images          VARCHAR(1024)   DEFAULT NULL COMMENT '图片URL列表，JSON数组，最多3张',
    comment_enabled TINYINT(1)      NOT NULL DEFAULT 1,
    like_enabled    TINYINT(1)      NOT NULL DEFAULT 1,
    visibility      VARCHAR(16)     NOT NULL DEFAULT 'PUBLIC'
                    COMMENT 'PUBLIC / PRIVATE',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                    COMMENT 'DRAFT / PENDING / PUBLISHED / REJECTED',
    like_count      INT             NOT NULL DEFAULT 0,
    comment_count   INT             NOT NULL DEFAULT 0,
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_category_status (category, status),
    INDEX idx_status_created (status, created_at),
    INDEX idx_like_count (like_count),
    INDEX idx_comment_count (comment_count),
    CONSTRAINT fk_treehole_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='树洞动态表';

-- 树洞评论表
CREATE TABLE tb_treehole_comment (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    anonymous_name  VARCHAR(32)     NOT NULL,
    content         VARCHAR(100)    NOT NULL,
    parent_id       BIGINT          DEFAULT NULL COMMENT '父评论ID，NULL为顶级评论',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING / PUBLISHED / REJECTED',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES tb_treehole_post(id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='树洞评论表';

-- 树洞点赞表
CREATE TABLE tb_treehole_like (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_post_user (post_id, user_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_like_post FOREIGN KEY (post_id) REFERENCES tb_treehole_post(id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='树洞点赞表';
```

### 2.3 找搭子模块表

```sql
-- 搭子需求表
CREATE TABLE tb_partner_req (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    type            VARCHAR(16)     NOT NULL
                    COMMENT 'study / sport / meal / exam / travel / other',
    description     VARCHAR(200)    NOT NULL,
    conditions      VARCHAR(512)    NOT NULL COMMENT '匹配条件，JSON对象',
    images          VARCHAR(512)    DEFAULT NULL COMMENT '图片，JSON数组，最多2张',
    valid_days      TINYINT         NOT NULL DEFAULT 3 COMMENT '有效期天数，1-7',
    max_members     TINYINT         NOT NULL DEFAULT 5 COMMENT '最大匹配人数',
    visibility      VARCHAR(16)     NOT NULL DEFAULT 'sameSchool'
                    COMMENT 'all / sameSchool / sameMajor',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING / PUBLISHED / EXPIRED / CLOSED',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0,
    expire_time     DATETIME        NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_type_status (type, status),
    INDEX idx_status_expire (status, expire_time),
    INDEX idx_visibility (visibility),
    FULLTEXT INDEX ft_description (description),
    CONSTRAINT fk_partner_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子需求表';

-- 搭子匹配表
CREATE TABLE tb_partner_match (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    request_id      BIGINT          NOT NULL,
    applicant_id    BIGINT          NOT NULL COMMENT '申请人用户ID',
    apply_message   VARCHAR(100)    DEFAULT NULL COMMENT '申请附言',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING / ACCEPTED / REJECTED / ENDED',
    apply_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time   DATETIME        DEFAULT NULL,
    end_time        DATETIME        DEFAULT NULL,
    end_reason      VARCHAR(100)    DEFAULT NULL,

    UNIQUE KEY uk_req_applicant (request_id, applicant_id),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_status (status),
    CONSTRAINT fk_match_req FOREIGN KEY (request_id) REFERENCES tb_partner_req(id),
    CONSTRAINT fk_match_applicant FOREIGN KEY (applicant_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子匹配表';

-- 搭子聊天表
CREATE TABLE tb_partner_chat (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    match_id        BIGINT          NOT NULL,
    sender_id       BIGINT          NOT NULL,
    content         VARCHAR(500)    NOT NULL,
    content_type    VARCHAR(16)     NOT NULL DEFAULT 'text'
                    COMMENT 'text / image / emoji',
    is_read         TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_match_id (match_id),
    INDEX idx_match_time (match_id, created_at),
    CONSTRAINT fk_chat_match FOREIGN KEY (match_id) REFERENCES tb_partner_match(id),
    CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子聊天表';

-- 搭子评价表
CREATE TABLE tb_partner_review (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    match_id        BIGINT          NOT NULL,
    reviewer_id     BIGINT          NOT NULL COMMENT '评价人',
    target_id       BIGINT          NOT NULL COMMENT '被评价人',
    rating          TINYINT         NOT NULL COMMENT '1-5星',
    content         VARCHAR(100)    NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_match_reviewer (match_id, reviewer_id),
    INDEX idx_target_id (target_id),
    CONSTRAINT fk_review_match FOREIGN KEY (match_id) REFERENCES tb_partner_match(id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES tb_user(id),
    CONSTRAINT fk_review_target FOREIGN KEY (target_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子评价表';
```

### 2.4 恋爱互助模块表

```sql
-- 交友资料表
CREATE TABLE tb_love_profile (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    gender          VARCHAR(8)      NOT NULL COMMENT 'male / female',
    age             TINYINT         NOT NULL,
    height          SMALLINT        DEFAULT NULL COMMENT '身高cm',
    weight          SMALLINT        DEFAULT NULL COMMENT '体重kg',
    constellation   VARCHAR(8)      DEFAULT NULL,
    interests       VARCHAR(512)    DEFAULT NULL COMMENT 'JSON数组',
    mate_preference VARCHAR(512)    NOT NULL COMMENT '择偶标准，JSON',
    declaration     VARCHAR(100)    NOT NULL COMMENT '交友宣言',
    photos          VARCHAR(1024)   DEFAULT NULL COMMENT '照片URL，JSON数组，最多5张',
    visibility      VARCHAR(16)     NOT NULL DEFAULT 'all'
                    COMMENT 'all / matched / selfOnly',
    completeness    TINYINT         NOT NULL DEFAULT 0 COMMENT '资料完整度百分比',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_love_profile_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交友资料表';

-- 交友需求表
CREATE TABLE tb_love_req (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    profile_id      BIGINT          NOT NULL,
    description     VARCHAR(200)    NOT NULL,
    valid_days      TINYINT         NOT NULL DEFAULT 7 COMMENT '1-14天',
    scope           VARCHAR(16)     NOT NULL DEFAULT 'sameSchool'
                    COMMENT 'sameSchool / sameCity / all',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING / PUBLISHED / EXPIRED / CLOSED',
    expire_time     DATETIME        NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_status_scope (status, scope),
    INDEX idx_expire_time (expire_time),
    CONSTRAINT fk_love_req_user FOREIGN KEY (user_id) REFERENCES tb_user(id),
    CONSTRAINT fk_love_req_profile FOREIGN KEY (profile_id) REFERENCES tb_love_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交友需求表';

-- 交友匹配表（双向心动）
CREATE TABLE tb_love_match (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    request_id      BIGINT          NOT NULL,
    applicant_id    BIGINT          NOT NULL COMMENT '发起心动方',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING / MUTUAL / REJECTED / ENDED',
    apply_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time   DATETIME        DEFAULT NULL COMMENT '对方回应时间',
    end_time        DATETIME        DEFAULT NULL,

    UNIQUE KEY uk_req_applicant (request_id, applicant_id),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_status (status),
    CONSTRAINT fk_love_match_req FOREIGN KEY (request_id) REFERENCES tb_love_req(id),
    CONSTRAINT fk_love_match_applicant FOREIGN KEY (applicant_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交友匹配表';

-- 交友聊天表
CREATE TABLE tb_love_chat (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    match_id        BIGINT          NOT NULL,
    sender_id       BIGINT          NOT NULL,
    content         VARCHAR(500)    NOT NULL,
    content_type    VARCHAR(16)     NOT NULL DEFAULT 'text'
                    COMMENT 'text / image / emoji / voice',
    voice_url       VARCHAR(255)    DEFAULT NULL COMMENT '语音消息URL',
    voice_duration  SMALLINT        DEFAULT NULL COMMENT '语音时长秒数，≤60s',
    is_read         TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_match_id (match_id),
    INDEX idx_match_time (match_id, created_at),
    CONSTRAINT fk_love_chat_match FOREIGN KEY (match_id) REFERENCES tb_love_match(id),
    CONSTRAINT fk_love_chat_sender FOREIGN KEY (sender_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交友聊天表';

-- 交友评价表
CREATE TABLE tb_love_review (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    match_id        BIGINT          NOT NULL,
    reviewer_id     BIGINT          NOT NULL,
    target_id       BIGINT          NOT NULL,
    rating          TINYINT         NOT NULL COMMENT '1-5星',
    content         VARCHAR(100)    NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_match_reviewer (match_id, reviewer_id),
    INDEX idx_target_id (target_id),
    CONSTRAINT fk_love_review_match FOREIGN KEY (match_id) REFERENCES tb_love_match(id),
    CONSTRAINT fk_love_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES tb_user(id),
    CONSTRAINT fk_love_review_target FOREIGN KEY (target_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交友评价表';
```

### 2.5 消息通知 & 审核 & 反馈表

```sql
-- 通知表
CREATE TABLE tb_notification (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    type            VARCHAR(16)     NOT NULL
                    COMMENT 'system / treehole_like / treehole_comment / '
                            'partner_apply / partner_accept / partner_reject / '
                            'love_match / love_mutual / review / admin',
    title           VARCHAR(64)     NOT NULL,
    content         VARCHAR(256)    NOT NULL,
    related_type    VARCHAR(32)     DEFAULT NULL COMMENT '关联内容类型',
    related_id      BIGINT          DEFAULT NULL COMMENT '关联内容ID（可跳转）',
    is_read         TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_read_time (user_id, is_read, created_at),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 消息设置表
CREATE TABLE tb_message_setting (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT      NOT NULL,
    system_enabled      TINYINT(1)  NOT NULL DEFAULT 1,
    interaction_enabled TINYINT(1)  NOT NULL DEFAULT 1,
    alert_type          VARCHAR(16) NOT NULL DEFAULT 'sound'
                        COMMENT 'vibrate / sound / silent',
    disturb_start       TIME        DEFAULT NULL COMMENT '免打扰开始时间',
    disturb_end         TIME        DEFAULT NULL COMMENT '免打扰结束时间',

    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_msg_setting_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息设置表';

-- 审核记录表
CREATE TABLE tb_review_record (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    content_type    VARCHAR(32)     NOT NULL
                    COMMENT 'treehole_post / treehole_comment / partner_req / love_profile / love_req',
    content_id      BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL COMMENT '内容发布者',
    reviewer_id     BIGINT          DEFAULT NULL COMMENT '审核员（NULL表示自动审核）',
    result          VARCHAR(16)     NOT NULL
                    COMMENT 'PASSED / REJECTED / WARNING',
    comment         VARCHAR(256)    DEFAULT NULL COMMENT '审核意见',
    submit_time     DATETIME        NOT NULL,
    review_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_content (content_type, content_id),
    INDEX idx_reviewer (reviewer_id),
    INDEX idx_result_time (result, review_time),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录表';

-- 敏感词库表
CREATE TABLE tb_sensitive_word (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    word            VARCHAR(64)     NOT NULL,
    category        VARCHAR(16)     NOT NULL DEFAULT 'general'
                    COMMENT 'general / adult / violence / spam / abuse',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_word (word),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库表';

-- 反馈建议表
CREATE TABLE tb_feedback (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    type            VARCHAR(16)     NOT NULL
                    COMMENT 'bug / content / privacy / suggestion / other',
    content         VARCHAR(500)    NOT NULL,
    images          VARCHAR(512)    DEFAULT NULL COMMENT '证据图片，JSON数组',
    contact         VARCHAR(64)     DEFAULT NULL COMMENT '联系方式（可选）',
    feedback_number VARCHAR(20)     NOT NULL COMMENT '反馈编号（如FB20260513001）',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING / VIEWED / PROCESSING / RESOLVED / UNSOLVABLE',
    process_comment VARCHAR(256)    DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    DATETIME        DEFAULT NULL,

    UNIQUE KEY uk_feedback_number (feedback_number),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈建议表';
```

---

## 三、索引设计说明

### 3.1 索引设计原则

| 原则 | 说明 |
|------|------|
| 主键索引 | 所有表使用自增 BIGINT 主键，InnoDB 自动创建聚簇索引 |
| 唯一索引 | 业务唯一字段（phone, username, student_id 等）使用 UNIQUE KEY |
| 查询条件索引 | WHERE 子句高频字段（status, type, category 等）添加普通索引 |
| 排序索引 | ORDER BY 高频字段（created_at, like_count 等）添加索引 |
| 外键索引 | 所有外键列自动添加索引（MySQL InnoDB 要求） |
| 联合索引 | 多条件查询场景使用联合索引（如 status + created_at） |

### 3.2 关键索引说明

| 表 | 索引 | 类型 | 理由 |
|----|------|------|------|
| tb_user | uk_phone, uk_username | UNIQUE | 登录/注册时按手机号或用户名查找，高频查询 |
| tb_treehole_post | idx_category_status | 联合 | 按分类+状态筛选动态，首页核心查询 |
| tb_treehole_post | idx_status_created | 联合 | 按状态+时间排序，列表分页核心查询 |
| tb_treehole_post | idx_like_count | 普通 | 按热度排序（按点赞数），高频排序字段 |
| tb_treehole_like | uk_post_user | UNIQUE | 防止重复点赞，同时支持查询某用户点赞列表 |
| tb_partner_req | idx_type_status | 联合 | 按类型+状态筛选搭子需求 |
| tb_partner_req | ft_description | FULLTEXT | 搭子需求关键词搜索，使用全文索引比 LIKE 效率高 |
| tb_partner_match | uk_req_applicant | UNIQUE | 防止同一用户重复申请同一需求 |
| tb_notification | idx_user_read_time | 联合 | 获取用户未读通知，按时间排序，最频繁的查询 |
| tb_review_record | idx_content | 联合 | 按内容类型+ID查询审核记录 |

### 3.3 不使用索引的场景

- 枚举值少的字段（如 `TINYINT(1)` 的布尔字段）不建单列索引
- 频繁更新的字段慎重建索引（维护代价 > 查询收益）
- 小表（< 1000 行）不建过多索引

---

## 四、隐私数据与安全设计

### 4.1 密码存储

- 使用 **BCrypt** 算法对密码进行哈希存储
- 盐值由 BCrypt 自动生成，不单独存储
- cost factor 设置为 10~12
- 前端传输密码时使用 HTTPS 加密传输，不额外使用 RSA（避免增加复杂度）

### 4.2 敏感数据加密

| 字段 | 存储方式 | 加密算法 | 说明 |
|------|---------|---------|------|
| phone（手机号） | AES-256 加密存储 | AES/CBC/PKCS5Padding | 密钥托管在 KMS 或环境变量中 |
| student_id（学号） | AES-256 加密存储 | AES/CBC/PKCS5Padding | 同上 |
| real_name（真实姓名） | AES-256 加密存储 | AES/CBC/PKCS5Padding | 同上 |
| id_card（身份证号） | AES-256 加密存储 | AES/CBC/PKCS5Padding | 同上，仅认证时使用 |

### 4.3 展示脱敏规则

| 字段 | 展示格式 | 示例 |
|------|---------|------|
| 手机号 | 前3+****+后4 | 138****8000 |
| 身份证号 | 前1后1+**** | 3***********1 |
| 学号 | 前2+****+后2 | 20****01 |

---

## 五、AI 辅助审查记录

### 5.1 第三范式 (3NF) 检查

| 检查项 | AI 原始设计 | 审查结果 | 修正 |
|--------|-----------|---------|------|
| 是否满足 3NF | AI设计基本满足3NF | 通过 | — |
| 部分依赖问题 | tb_user 中 university/major 依赖于 student_id 而非主键 | 违反 2NF | 将这些字段移至 tb_user_cert |
| 传递依赖 | AI在 tb_love_req 中存储了 user 的 profile 字段（冗余） | 违反 3NF | 仅存 profile_id 外键 |

### 5.2 索引合理性审查

| 审查点 | AI 原始设计 | 问题 | 修正 |
|--------|-----------|------|------|
| tb_partner_req 描述搜索 | 使用普通 INDEX | LIKE '%keyword%' 无法走索引 | 改为 FULLTEXT 全文索引 |
| 未建联合索引 | AI 在每个字段上分别建了单列索引 | 多条件查询时只能用一个索引 | 根据查询模式设计联合索引 |
| 缺失唯一约束 | tb_treehole_like 未建唯一索引 | 可能产生重复点赞数据 | 增加 uk_post_user |

### 5.3 性能瓶颈分析

| 潜在瓶颈 | 原因 | 优化方案 |
|---------|------|---------|
| 树洞首页查询 | 按热度排序需扫描大量数据 | 方案①（当前）：MySQL 分页+索引优化，控制每页数据量；方案②（后期）：按 ADR-005，若性能不足可引入 Redis 缓存热门动态 |
| 搭子匹配计算 | 实时匹配遍历所有有效需求 | 当前采用带索引的 SQL 条件查询 + 分页；后期若数据量增长可引入 Elasticsearch |
| 通知写入 | 高并发点赞产生大量通知写入 | 当前阶段本地同步写入通知（符合 P2 最简架构决策）；后期可异步化（消息队列） |
| 敏感词检测 | 每次发布内容遍历全词库 | 使用前缀树（Trie）或 AC 自动机优化匹配效率（应用层优化，无需外部组件） |
