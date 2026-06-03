-- 1. 创建核心用户表
drop table if exists `t_user`;
CREATE TABLE `t_user` (
                          `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                          `username` varchar(50) NOT NULL COMMENT '用户名',
                          `phone` varchar(20)  NOT NULL COMMENT '手机号',
                          `password` varchar(255) NOT NULL COMMENT '密码',
                          `role` varchar(50) NOT NULL COMMENT '用户角色(JOB_SEEKER //求职者, RECRUITER//招聘者, COMP_ADMIN//企业管理员, SYS_ADMIN//系统管理员)',
                          `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                          `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
                          `profile` varchar(512) DEFAULT NULL COMMENT '个人简介',
                          `account_status` tinyint NOT NULL DEFAULT '1' COMMENT '账户状态(0禁用,1正常,2锁定,3注销)',
                          `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                          `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删,1已删)',
                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          `edit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '系统更新时间',
                          `company_id` bigint DEFAULT NULL COMMENT '用户所属公司ID',
                          PRIMARY KEY (`user_id`),
                          INDEX idx_phone (`phone`),
                          INDEX idx_username (`username`),
                          INDEX idx_email (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 创建公司基本信息表
CREATE TABLE `t_company` (
    `company_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公司ID',
    `admin_id` bigint NOT NULL COMMENT '企业管理员ID 对应用户表user_id',
    `company_name` varchar(100) NOT NULL COMMENT '公司全称',
    `logo_url` varchar(500) DEFAULT NULL COMMENT 'Logo URL',
    `website` varchar(200) DEFAULT NULL COMMENT '官网',
    `introduction` text COMMENT '公司简介',
    `industry` varchar(100) DEFAULT NULL COMMENT '所属行业',
    `scale` varchar(20) DEFAULT NULL COMMENT '公司规模',
    `city` varchar(50) DEFAULT NULL COMMENT '所在城市',
#     `longitude`  DECIMAL(10, 7) NULL COMMENT '经度', 后续扩展
#     `latitude` DECIMAL(10, 7) NULL COMMENT '纬度', 后续扩展
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0待审,1正常,2禁用,3注销)',
    `business_license_url` varchar(500) NOT NULL COMMENT '营业执照URL',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `edit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '系统更新时间',
    PRIMARY KEY (`company_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_industry` (`industry`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司基本信息表';


-- 3. 创建公司资质认证记录表
CREATE TABLE `t_company_certification_record` (
   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资质记录ID',
   `company_id` bigint NOT NULL COMMENT '公司ID',
   `admin_id` bigint NOT NULL COMMENT '企业管理员ID 对应用户表user_id',
   `contact_name` varchar(50) NOT NULL COMMENT '联系人',
   `contact_phone` varchar(20) NOT NULL COMMENT '联系人电话',
   `contact_email` varchar(100) NOT NULL COMMENT '联系人邮箱',
   `contact_position` varchar(50) DEFAULT NULL COMMENT '联系人职位',
   `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0待审,1通过,2驳回,3取消)',
   `review_notes` varchar(500) DEFAULT NULL COMMENT '审核意见',
   `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
   `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
   `reviewed_time` datetime DEFAULT NULL COMMENT '审核时间',
   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
   PRIMARY KEY (`id`),
   INDEX `idx_company_id` (`company_id`),
   INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司资质认证记录表';

-- 4. 创建招聘者绑定公司的记录表
CREATE TABLE `t_binding_request` (
     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请记录ID',
     `user_id` bigint NOT NULL COMMENT '申请人ID（招聘者）',
     `company_id` bigint NOT NULL COMMENT '目标公司ID',
     `application_notes` text COMMENT '申请备注',
     `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0待审,1通过,2拒绝,3取消)',
     `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID（公司管理员）',
     `review_notes` text COMMENT '审核意见',
     `reviewed_time` datetime DEFAULT NULL COMMENT '审核时间',
     `expires_at` datetime NOT NULL COMMENT '申请失效时间',
     `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (`id`),
     INDEX `idx_applicant_id` (`user_id`),
     INDEX `idx_company_id` (`company_id`),
     INDEX `idx_status` (`status`),
     INDEX `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘者绑定公司申请记录表';


-- 5. 创建核心职位表
drop table if exists `t_job_position`;
CREATE TABLE `t_job_position` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `company_id` bigint NOT NULL COMMENT '发布公司',
    `title` varchar(200) NOT NULL COMMENT '职位标题',
    `description` longtext NOT NULL COMMENT '职位描述',
    `requirement` longtext NOT NULL COMMENT '职位要求',
    `work_city` varchar(50) NOT NULL COMMENT '工作城市',
    `job_type` tinyint NOT NULL DEFAULT '0 ' COMMENT '工作性质(0全职,1兼职,2实习,3远程 )',
    `tags` varchar(512)  NULL COMMENT '职位标签{JSON数组}',
    `min_salary` int DEFAULT NULL COMMENT '最低薪资',
    `max_salary` int DEFAULT NULL COMMENT '最高薪资',
    `experience` varchar(20) DEFAULT NULL COMMENT '经验要求',
    `education` varchar(20) DEFAULT NULL COMMENT '学历要求',
    `headcount` int NOT NULL DEFAULT '1' COMMENT '招聘人数',
    `hiring_manager_id` bigint DEFAULT NULL COMMENT '负责人',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '职位状态(0草稿,1招聘中,2已暂停,3已招满,4已关闭)',
    `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
    `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
    `apply_count` int NOT NULL DEFAULT '0' COMMENT '投递量',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_company_id` (`company_id`),
    INDEX `idx_status_publish` (`status`, `publish_time` DESC),
    INDEX `x_city_job_type` (`work_city`, `job_type`),
    INDEX `idx_hiring_manager` (`hiring_manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位发布表';

-- 6. 简历主表基本信息
drop table if exists `t_resume`;
CREATE TABLE `t_resume`(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `template_id` int NOT NULL COMMENT '简历模板ID',
    `name` varchar(50) NOT NULL COMMENT '姓名',
    `gender` tinyint NOT NULL COMMENT '性别(0男,1女,2保密)',
    `birthday` date DEFAULT NULL COMMENT '出生日期',
    `phone` varchar(20) NOT NULL COMMENT '手机号',
    `email` varchar(100) NOT NULL COMMENT '邮箱',
    `address` varchar(200) DEFAULT NULL COMMENT '现居地址',
    `avatar` varchar(200) DEFAULT NULL COMMENT '头像',
    `city` varchar(50) DEFAULT NULL COMMENT '期望工作城市',
    `summary` varchar(500) DEFAULT NULL COMMENT '个人简介/求职意向',
    `is_default` tinyint NOT NULL COMMENT '是否默认简历（用于多份简历管理）',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
);
-- 6.1 教育经历表关联简历表
drop table if exists `t_resume_education`;
CREATE TABLE `t_resume_education`(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resume_id` bigint NOT NULL COMMENT '简历ID',
    `school` varchar(100) NOT NULL COMMENT '学校名称',
    `major` varchar(100) NOT NULL COMMENT '专业',
    `degree` varchar(20) NOT NULL COMMENT '学历',
    `start_date` datetime NOT NULL COMMENT '开始时间',
    `end_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '毕业时间（或至今）',
    `description` TEXT COMMENT '在校经历、荣誉等',
    PRIMARY KEY (`id`),
    INDEX `idx_resume_id` (`resume_id`)
);
-- 6.2 工作经历表关联简历表
drop table if exists `t_resume_experience`;
CREATE TABLE `t_resume_experience`(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resume_id` bigint NOT NULL COMMENT '简历ID',
    `company` varchar(100) NOT NULL COMMENT '公司名称',
    `position` varchar(100) NOT NULL COMMENT '职位名称',
    `start_date` datetime NOT NULL COMMENT '开始时间',
    `end_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间（或至今）',
    `description` TEXT COMMENT '工作内容、业绩等',
    PRIMARY KEY (`id`),
    INDEX `idx_resume_id` (`resume_id`)
);
-- 6.3 项目经历表关联简历表
drop table if exists `t_resume_project`;
CREATE TABLE `t_resume_project`(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resume_id` bigint NOT NULL COMMENT '简历ID',
    `name` varchar(100) NOT NULL COMMENT '项目名称',
    `description` TEXT COMMENT '项目描述',
    `start_date` datetime NOT NULL COMMENT '开始时间',
    `end_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间（或至今）',
    PRIMARY KEY (`id`),
    INDEX `idx_resume_id` (`resume_id`)
 );

# 7. 简历投递记录表
drop table if exists `t_job_application`;
CREATE TABLE `t_job_application` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '投递记录ID',
    `company_id` bigint NOT NULL COMMENT '投递的公司',
    `job_position_id` bigint NOT NULL COMMENT '职位ID',
    `user_id` bigint NOT NULL COMMENT '求职者用户ID',
    `resume_id` bigint DEFAULT NULL COMMENT '投递时使用的简历ID',
    `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0待处理,1已查看,2初筛通过,3初筛不通过,4面试中,5已发Offer,6已录用,7已淘汰)',
#     `type` tinyint NOT NULL DEFAULT '1' COMMENT '类型(1主动投递,2HR邀请,3内部推荐)',
#     `source` varchar(50) DEFAULT NULL COMMENT '来源渠道',
    `cover_letter` text COMMENT '求职信',
    `remarks` text COMMENT 'HR备注',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_position_id` (`job_position_id`),
    kEY `idx_company_id` (`company_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_apply_time` (`apply_time`),
    KEY `idx_job_user` (`job_position_id`, `user_id`) -- 联合索引，加速查重
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历投递记录表';

# 8. 聊天记录表
drop table if exists `t_chat_message`;
CREATE TABLE `t_chat_message` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `send_id` bigint NOT NULL COMMENT '发送者ID',
    `receive_id` bigint NOT NULL COMMENT '接收者ID',
    `content` text NOT NULL COMMENT '消息内容',
    `timestamp` bigint NOT NULL COMMENT '客户端发送时间戳',
    `sent_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端发送时间',
    `read_time` datetime DEFAULT NULL COMMENT '已读时间',
    `revoke_time` datetime DEFAULT NULL COMMENT '撤回时间',
    `msg_type` tinyint NOT NULL DEFAULT '0' COMMENT '消息类型(0文本,1系统消息)',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '消息状态(未读0,已读1,撤回2)',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_send_id` (`send_id`),
    KEY `idx_receive_id` (`receive_id`),
    KEY `idx_timestamp` (`timestamp`),
    KEY `idx_unread` (`receive_id`, `status`, `is_deleted`, `sent_time`),
    KEY `idx_dialog_sr` (`send_id`, `receive_id`, `sent_time`),
    KEY `idx_dialog_rs` (`receive_id`, `send_id`, `sent_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天记录表';

# 9. 面试预约主表
drop table if exists `t_interview_notice`;
CREATE TABLE `t_interview_notice` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_application_id` bigint NOT NULL COMMENT '投递记录ID',
    `company_id` bigint NOT NULL COMMENT '公司ID',
    `job_seeker_id` bigint NOT NULL COMMENT '求职者用户ID',
    `creator_id` bigint NOT NULL COMMENT '创建人ID（招聘者）',
    `interview_type` tinyint NOT NULL DEFAULT '1' COMMENT '面试类型(0线下面试,1线上面试)',
    `interview_start_time` datetime NOT NULL COMMENT '面试开始时间',
    `interview_end_time` datetime NOT NULL COMMENT '面试结束时间',
    `interview_address` varchar(255) DEFAULT NULL COMMENT '线下面试地址',
    `rtc_platform` tinyint DEFAULT NULL COMMENT '线上会议平台(1TRTC,2ZEGO,3Agora,4Jitsi)',
    `rtc_room_id` varchar(128) DEFAULT NULL COMMENT '会议房间号',
    `rtc_room_name` varchar(128) DEFAULT NULL COMMENT '会议名称',
    `rtc_join_url` varchar(500) DEFAULT NULL COMMENT '会议加入链接',
    `rtc_password` varchar(100) DEFAULT NULL COMMENT '会议密码/入会口令',
    `comment` varchar(500) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0待确认,1已确认,2已拒绝,3已取消,4已结束)',
    `candidate_reply_reason` varchar(500) DEFAULT NULL COMMENT '求职者拒绝原因',
    `candidate_reply_time` datetime DEFAULT NULL COMMENT '求职者回复时间',
    `cancel_reason` varchar(500) DEFAULT NULL COMMENT '招聘者取消原因',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_application_id` (`job_application_id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_job_seeker_id` (`job_seeker_id`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_interview_start_time` (`interview_start_time`),
    KEY `idx_interview_end_time` (`interview_end_time`),
    KEY `idx_status_deleted_end_time` (`status`, `is_deleted`, `interview_end_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试预约主表';

# 9.1 面试参与人表
drop table if exists `t_interview_notice_participant`;
CREATE TABLE `t_interview_notice_participant` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interview_notice_id` bigint NOT NULL COMMENT '面试预约ID',
    `user_id` bigint NOT NULL COMMENT '参与人用户ID',
    `participant_type` tinyint NOT NULL DEFAULT '1' COMMENT '参与人类型(1面试官,2候选人)',
    `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否主面试官',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`interview_notice_id`, `user_id`),
    KEY `idx_notice_id` (`interview_notice_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试参与人表';

# 10. Offer记录表
drop table if exists `t_offer_record`;
CREATE TABLE `t_offer_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_application_id` bigint NOT NULL COMMENT '投递记录ID',
    `company_id` bigint NOT NULL COMMENT '公司ID',
    `job_seeker_id` bigint NOT NULL COMMENT '求职者用户ID',
    `recruiter_id` bigint NOT NULL COMMENT '发送Offer的招聘者ID',
    `interview_notice_id` bigint DEFAULT NULL COMMENT '关联面试通知ID',
    `job_position_id` bigint NOT NULL COMMENT '职位ID',
    `expected_entry_date` date DEFAULT NULL COMMENT '预计入职日期',
    `salary_monthly` decimal(10,2) DEFAULT NULL COMMENT '税前月薪',
    `salary_month_count` tinyint DEFAULT NULL COMMENT '年薪月数（如13薪）',
    `probation_months` tinyint DEFAULT NULL COMMENT '试用期（月）',
    `offer_deadline` datetime NOT NULL COMMENT 'Offer确认截止时间',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0待确认,1已接受,2已拒绝,3已过期,4已撤回,5已取消)',
    `welfare` varchar(500) DEFAULT NULL COMMENT '福利说明',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
    `respond_time` datetime DEFAULT NULL COMMENT '候选人响应时间',
    `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_application_id` (`job_application_id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_job_seeker_id` (`job_seeker_id`),
    KEY `idx_recruiter_id` (`recruiter_id`),
    KEY `idx_job_position_id` (`job_position_id`),
    KEY `idx_offer_deadline` (`offer_deadline`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Offer记录表';

-- 11. PostgreSQL 向量推荐库表结构（独立数据库执行）
-- 需要先在 PostgreSQL 中安装 pgvector 扩展：CREATE EXTENSION IF NOT EXISTS vector;
DROP TABLE IF EXISTS t_job_embedding;
CREATE TABLE t_job_embedding (
    job_id BIGINT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    job_title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1024) NOT NULL,
    embedding_model VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_embedding_company_id ON t_job_embedding (company_id);
CREATE INDEX idx_job_embedding_vector ON t_job_embedding USING hnsw (embedding vector_cosine_ops);

