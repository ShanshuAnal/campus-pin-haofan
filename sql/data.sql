-- Campus Pin Hao Fan demo data.
-- Run after sql/schema.sql.
-- notification_record is enhancement data; MVP main flow does not depend on it.
-- Demo user password_hash values are placeholders for structure/demo data.
-- Actual backend tests should create users through POST /api/auth/register
-- so the password hash matches the implemented hashing algorithm.

SET NAMES utf8mb4;

INSERT INTO `user` (
  `id`, `account`, `password_hash`, `nickname`, `phone`, `status`,
  `last_login_time`, `last_login_ip`, `password_update_time`, `create_time`, `update_time`
) VALUES
(1001, '20260001', '$2a$10$demoPasswordHashForDevOnly000000000000000000000000000001', '何帆', '13800000001', 'ACTIVE',
 '2026-05-26 12:00:00', '127.0.0.1', '2026-05-26 08:00:00', '2026-05-26 08:00:00', '2026-05-26 12:00:00'),
(1002, '20260002', '$2a$10$demoPasswordHashForDevOnly000000000000000000000000000002', '小雨', '13800000002', 'ACTIVE',
 '2026-05-26 12:05:00', '127.0.0.1', '2026-05-26 08:00:00', '2026-05-26 08:00:00', '2026-05-26 12:05:00'),
(1003, '20260003', '$2a$10$demoPasswordHashForDevOnly000000000000000000000000000003', '阿明', '13800000003', 'ACTIVE',
 NULL, NULL, '2026-05-26 08:00:00', '2026-05-26 08:00:00', '2026-05-26 08:00:00'),
(1004, '20260004', '$2a$10$demoPasswordHashForDevOnly000000000000000000000000000004', '林可', '13800000004', 'ACTIVE',
 '2026-05-26 21:50:00', '127.0.0.1', '2026-05-26 08:00:00', '2026-05-26 08:00:00', '2026-05-26 21:50:00');

INSERT INTO `user_refresh_token` (
  `id`, `user_id`, `token_hash`, `expire_time`, `revoked`, `revoked_time`, `create_time`, `update_time`
) VALUES
(1101, 1001, 'demo-refresh-token-hash-1001-active', '2026-06-02 12:00:00', 0, NULL, '2026-05-26 12:00:00', '2026-05-26 12:00:00'),
(1102, 1002, 'demo-refresh-token-hash-1002-active', '2026-06-02 12:05:00', 0, NULL, '2026-05-26 12:05:00', '2026-05-26 12:05:00'),
(1103, 1004, 'demo-refresh-token-hash-1004-revoked', '2026-06-02 21:50:00', 1, '2026-05-26 22:10:00', '2026-05-26 21:50:00', '2026-05-26 22:10:00');

INSERT INTO `group_order` (
  `id`, `title`, `order_type`, `merchant_name`, `pickup_location`, `creator_id`, `deadline_time`,
  `max_participants`, `participant_count`, `discount_threshold_amount`, `discount_amount`,
  `original_total_amount`, `actual_discount_amount`, `payable_total_amount`, `rounding_adjustment_amount`,
  `status`, `pickup_user_id`, `remark`, `locked_time`, `finish_time`, `cancel_time`, `create_time`, `update_time`
) VALUES
(2001, '奶茶满 60 减 10', 'MILK_TEA', '一教奶茶窗口', '一教大厅门口', 1001, '2026-05-26 12:30:00',
 5, 3, 60.00, 10.00, 68.00, 10.00, 58.00, 0.00,
 'ORDERED', 1001, '演示订单：已锁单并完成外部下单，等待取餐。', '2026-05-26 12:20:00', NULL, NULL, '2026-05-26 12:00:00', '2026-05-26 12:25:00'),
(2002, '夜宵拼炸鸡', 'MIDNIGHT_SNACK', '东门炸鸡店', '东门校门口', 1004, '2026-05-26 22:30:00',
 4, 1, 80.00, 15.00, 32.00, 0.00, 32.00, 0.00,
 'CREATED', NULL, '演示订单：仍可加入，配送费暂由发起人手动拆分进成员原始金额。', NULL, NULL, NULL, '2026-05-26 21:50:00', '2026-05-26 21:50:00');

INSERT INTO `order_participant` (
  `id`, `group_order_id`, `user_id`, `original_amount`, `discount_share_amount`,
  `payable_amount`, `rounding_adjustment_amount`, `payment_status`,
  `paid_mark_time`, `paid_confirm_time`, `join_time`, `remark`, `create_time`, `update_time`
) VALUES
(3001, 2001, 1001, 28.00, 4.12, 23.88, 0.00, 'CONFIRMED',
 '2026-05-26 12:26:00', '2026-05-26 12:27:00', '2026-05-26 12:01:00', '少冰。', '2026-05-26 12:01:00', '2026-05-26 12:27:00'),
(3002, 2001, 1002, 22.00, 3.24, 18.76, 0.00, 'PAID',
 '2026-05-26 12:28:00', NULL, '2026-05-26 12:05:00', '三分糖。', '2026-05-26 12:05:00', '2026-05-26 12:28:00'),
(3003, 2001, 1003, 18.00, 2.64, 15.36, 0.00, 'UNPAID',
 NULL, NULL, '2026-05-26 12:08:00', '正常冰。', '2026-05-26 12:08:00', '2026-05-26 12:20:00'),
(3004, 2002, 1004, 32.00, 0.00, 32.00, 0.00, 'UNPAID',
 NULL, NULL, '2026-05-26 21:51:00', '含手动分摊的包装费 2 元。', '2026-05-26 21:51:00', '2026-05-26 21:51:00');

INSERT INTO `meal_item` (
  `id`, `group_order_id`, `participant_id`, `item_name`, `quantity`, `unit_price`, `subtotal_amount`, `remark`, `create_time`, `update_time`
) VALUES
(4001, 2001, 3001, '杨枝甘露', 1, 18.00, 18.00, '少冰', '2026-05-26 12:01:00', '2026-05-26 12:01:00'),
(4002, 2001, 3001, '芋圆加料', 1, 10.00, 10.00, '加芋圆', '2026-05-26 12:01:00', '2026-05-26 12:01:00'),
(4003, 2001, 3002, '珍珠奶茶', 1, 22.00, 22.00, '三分糖', '2026-05-26 12:05:00', '2026-05-26 12:05:00'),
(4004, 2001, 3003, '柠檬茶', 1, 18.00, 18.00, '正常冰', '2026-05-26 12:08:00', '2026-05-26 12:08:00'),
(4005, 2002, 3004, '炸鸡腿套餐', 1, 30.00, 30.00, '微辣', '2026-05-26 21:51:00', '2026-05-26 21:51:00'),
(4006, 2002, 3004, '包装费手动分摊', 1, 2.00, 2.00, 'MVP 暂并入原始金额', '2026-05-26 21:51:00', '2026-05-26 21:51:00');

INSERT INTO `payment_record` (
  `id`, `group_order_id`, `participant_id`, `user_id`, `amount`, `payment_status`,
  `mark_time`, `confirm_user_id`, `confirm_time`, `remark`, `create_time`, `update_time`
) VALUES
(5001, 2001, 3001, 1001, 23.88, 'PAID',
 '2026-05-26 12:26:00', NULL, NULL, '成员标记已付款。', '2026-05-26 12:26:00', '2026-05-26 12:26:00'),
(5002, 2001, 3001, 1001, 23.88, 'CONFIRMED',
 '2026-05-26 12:26:00', 1001, '2026-05-26 12:27:00', '发起人确认付款。', '2026-05-26 12:27:00', '2026-05-26 12:27:00'),
(5003, 2001, 3002, 1002, 18.76, 'PAID',
 '2026-05-26 12:28:00', NULL, NULL, '成员已标记，待发起人确认。', '2026-05-26 12:28:00', '2026-05-26 12:28:00');

INSERT INTO `pickup_record` (
  `id`, `group_order_id`, `pickup_user_id`, `pickup_location`, `pickup_status`,
  `estimated_arrival_time`, `actual_arrival_time`, `picked_up_time`, `distributed_time`,
  `remark`, `create_time`, `update_time`
) VALUES
(6001, 2001, 1001, '一教大厅门口', 'WAITING_DELIVERY',
 '2026-05-26 12:45:00', NULL, NULL, NULL,
 '发起人负责取餐，等待商家出餐。', '2026-05-26 12:25:00', '2026-05-26 12:25:00');

INSERT INTO `order_status_log` (
  `id`, `group_order_id`, `operator_id`, `target_type`, `target_id`, `action_type`,
  `before_status`, `after_status`, `remark`, `create_time`, `update_time`
) VALUES
(7001, 2001, 1001, 'GROUP_ORDER', 2001, 'LOCK_ORDER',
 'CREATED', 'LOCKED', '锁单并生成金额分摊结果。', '2026-05-26 12:20:00', '2026-05-26 12:20:00'),
(7002, 2001, 1001, 'GROUP_ORDER', 2001, 'MARK_ORDERED',
 'LOCKED', 'ORDERED', '发起人完成外部下单。', '2026-05-26 12:25:00', '2026-05-26 12:25:00'),
(7003, 2001, 1002, 'PAYMENT', 3002, 'MARK_PAID',
 'UNPAID', 'PAID', '成员标记已付款。', '2026-05-26 12:28:00', '2026-05-26 12:28:00'),
(7004, 2001, 1001, 'PICKUP', 6001, 'UPDATE_PICKUP',
 'WAITING_ORDER', 'WAITING_DELIVERY', '外部下单后进入待出餐。', '2026-05-26 12:25:00', '2026-05-26 12:25:00');

INSERT INTO `notification_record` (
  `id`, `receiver_id`, `biz_type`, `biz_id`, `title`, `content`, `status`, `read_time`, `create_time`, `update_time`
) VALUES
(8001, 1003, 'PAYMENT', 2001, '待付款提醒', '奶茶满 60 减 10 拼单已锁单，请确认应付金额并标记付款。', 'UNREAD', NULL, '2026-05-26 12:30:00', '2026-05-26 12:30:00'),
(8002, 1002, 'PICKUP', 2001, '取餐进度更新', '发起人已完成外部下单，当前等待商家出餐。', 'READ', '2026-05-26 12:31:00', '2026-05-26 12:30:00', '2026-05-26 12:31:00');
