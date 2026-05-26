-- Campus Pin Hao Fan database schema.
-- Target database: MySQL 8.
-- Foreign key strategy: no physical foreign keys are declared in MVP.
-- Logical foreign key relations are documented in docs/03-数据库设计.md and represented by id fields and indexes.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `notification_record`;
DROP TABLE IF EXISTS `order_status_log`;
DROP TABLE IF EXISTS `pickup_record`;
DROP TABLE IF EXISTS `payment_record`;
DROP TABLE IF EXISTS `meal_item`;
DROP TABLE IF EXISTS `order_participant`;
DROP TABLE IF EXISTS `group_order`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `account` varchar(64) NOT NULL COMMENT '登录账号或学号类唯一标识',
  `password_hash` varchar(255) NOT NULL COMMENT 'MVP 轻量真实登录使用的加密密码哈希',
  `nickname` varchar(64) NOT NULL COMMENT '昵称或姓名',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系方式',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_account` (`account`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE `group_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '拼单 ID',
  `title` varchar(128) NOT NULL COMMENT '拼单标题',
  `order_type` varchar(32) NOT NULL COMMENT '拼单类型',
  `merchant_name` varchar(128) NOT NULL COMMENT '商家或窗口名称',
  `pickup_location` varchar(128) NOT NULL COMMENT '拼单约定取餐地点',
  `creator_id` bigint NOT NULL COMMENT '发起人用户 ID，逻辑关联 user.id',
  `deadline_time` datetime NOT NULL COMMENT '加入截止时间',
  `max_participants` int DEFAULT NULL COMMENT '最大参与人数，空表示不限制',
  `participant_count` int NOT NULL DEFAULT 0 COMMENT '当前参与人数',
  `discount_threshold_amount` decimal(10,2) DEFAULT NULL COMMENT '满减门槛',
  `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '配置优惠金额',
  `original_total_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '原始总金额',
  `actual_discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际生效优惠金额',
  `payable_total_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '最终应付总金额',
  `rounding_adjustment_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '尾差调整金额',
  `status` varchar(32) NOT NULL DEFAULT 'CREATED' COMMENT '拼单状态',
  `pickup_user_id` bigint DEFAULT NULL COMMENT '当前取餐人用户 ID，逻辑关联 user.id',
  `remark` varchar(512) DEFAULT NULL COMMENT '拼单备注',
  `locked_time` datetime DEFAULT NULL COMMENT '锁单时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_order_status_deadline` (`status`, `deadline_time`),
  KEY `idx_group_order_status_create` (`status`, `create_time`),
  KEY `idx_group_order_creator` (`creator_id`, `create_time`),
  KEY `idx_group_order_pickup_user` (`pickup_user_id`, `status`),
  KEY `idx_group_order_type` (`order_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼单主表';

CREATE TABLE `order_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参与记录 ID',
  `group_order_id` bigint NOT NULL COMMENT '拼单 ID，逻辑关联 group_order.id',
  `user_id` bigint NOT NULL COMMENT '参与用户 ID，逻辑关联 user.id',
  `original_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '成员原始金额',
  `discount_share_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '成员分摊优惠金额',
  `payable_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '成员应付金额',
  `rounding_adjustment_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '成员尾差调整金额',
  `payment_status` varchar(32) NOT NULL DEFAULT 'UNPAID' COMMENT '付款状态',
  `paid_mark_time` datetime DEFAULT NULL COMMENT '成员标记付款时间',
  `paid_confirm_time` datetime DEFAULT NULL COMMENT '发起人确认付款时间',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入拼单时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '成员备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_participant_order_user` (`group_order_id`, `user_id`),
  KEY `idx_order_participant_user` (`user_id`, `create_time`),
  KEY `idx_order_participant_order` (`group_order_id`),
  KEY `idx_order_participant_payment` (`group_order_id`, `payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼单参与者表';

CREATE TABLE `meal_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '餐品条目 ID',
  `group_order_id` bigint NOT NULL COMMENT '拼单 ID，逻辑关联 group_order.id',
  `participant_id` bigint NOT NULL COMMENT '参与记录 ID，逻辑关联 order_participant.id',
  `item_name` varchar(128) NOT NULL COMMENT '餐品名称',
  `quantity` int NOT NULL COMMENT '数量',
  `unit_price` decimal(10,2) NOT NULL COMMENT '单价',
  `subtotal_amount` decimal(10,2) NOT NULL COMMENT '小计金额',
  `remark` varchar(512) DEFAULT NULL COMMENT '口味或备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meal_item_order` (`group_order_id`),
  KEY `idx_meal_item_participant` (`participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='餐品条目表';

CREATE TABLE `payment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '付款记录 ID',
  `group_order_id` bigint NOT NULL COMMENT '拼单 ID，逻辑关联 group_order.id',
  `participant_id` bigint NOT NULL COMMENT '参与记录 ID，逻辑关联 order_participant.id',
  `user_id` bigint NOT NULL COMMENT '付款成员用户 ID，逻辑关联 user.id',
  `amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '本次付款标记对应金额',
  `payment_status` varchar(32) NOT NULL COMMENT '本次记录后的付款状态',
  `mark_time` datetime DEFAULT NULL COMMENT '成员标记付款时间',
  `confirm_user_id` bigint DEFAULT NULL COMMENT '确认人用户 ID，逻辑关联 user.id',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认付款时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_payment_order` (`group_order_id`, `create_time`),
  KEY `idx_payment_participant` (`participant_id`, `create_time`),
  KEY `idx_payment_user` (`user_id`, `create_time`),
  KEY `idx_payment_status` (`group_order_id`, `payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='付款记录表';

CREATE TABLE `pickup_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '取餐记录 ID',
  `group_order_id` bigint NOT NULL COMMENT '拼单 ID，逻辑关联 group_order.id',
  `pickup_user_id` bigint NOT NULL COMMENT '取餐人用户 ID，逻辑关联 user.id',
  `pickup_location` varchar(128) DEFAULT NULL COMMENT '实际取餐地点或履约阶段地点信息',
  `pickup_status` varchar(32) NOT NULL DEFAULT 'WAITING_ORDER' COMMENT '取餐状态',
  `estimated_arrival_time` datetime DEFAULT NULL COMMENT '预计到达时间',
  `actual_arrival_time` datetime DEFAULT NULL COMMENT '实际到达时间',
  `picked_up_time` datetime DEFAULT NULL COMMENT '取餐完成时间',
  `distributed_time` datetime DEFAULT NULL COMMENT '分发完成时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '取餐备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pickup_order` (`group_order_id`),
  KEY `idx_pickup_user` (`pickup_user_id`, `pickup_status`),
  KEY `idx_pickup_status` (`pickup_status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='取餐记录表';

CREATE TABLE `order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '状态日志 ID',
  `group_order_id` bigint NOT NULL COMMENT '拼单 ID，逻辑关联 group_order.id',
  `operator_id` bigint NOT NULL COMMENT '操作人用户 ID，逻辑关联 user.id',
  `target_type` varchar(32) NOT NULL COMMENT '变更对象类型',
  `target_id` bigint NOT NULL COMMENT '变更对象 ID',
  `action_type` varchar(32) NOT NULL COMMENT '操作类型',
  `before_status` varchar(32) DEFAULT NULL COMMENT '变更前状态',
  `after_status` varchar(32) NOT NULL COMMENT '变更后状态',
  `remark` varchar(512) DEFAULT NULL COMMENT '操作备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_log_order` (`group_order_id`, `create_time`),
  KEY `idx_status_log_target` (`target_type`, `target_id`, `create_time`),
  KEY `idx_status_log_operator` (`operator_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼单状态日志表';

CREATE TABLE `notification_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知记录 ID',
  `receiver_id` bigint NOT NULL COMMENT '接收人用户 ID，逻辑关联 user.id',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务对象 ID',
  `title` varchar(128) NOT NULL COMMENT '通知标题',
  `content` varchar(512) NOT NULL COMMENT '通知内容',
  `status` varchar(32) NOT NULL DEFAULT 'UNREAD' COMMENT '通知状态',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification_receiver_status` (`receiver_id`, `status`, `create_time`),
  KEY `idx_notification_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知记录增强表，MVP 主流程可不依赖';
