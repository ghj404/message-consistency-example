-- 创建数据库
CREATE DATABASE IF NOT EXISTS message_consistency_example DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE message_consistency_example;

-- 业务表：订单表
DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_name` varchar(255) NOT NULL COMMENT '商品名称',
  `product_price` decimal(10,2) NOT NULL COMMENT '商品价格',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '购买数量',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 注意：消息一致性相关的表（mc_local_message、mc_message_archive、mc_alert_log）
-- 会由message-consistency-spring-boot-starter自动创建
