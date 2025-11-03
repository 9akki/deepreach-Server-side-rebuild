-- ----------------------------
-- Table structure for dr_price_config
-- ----------------------------
DROP TABLE IF EXISTS `dr_price_config`;
CREATE TABLE `dr_price_config` (
  `price_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '价格ID',
  `business_type` varchar(100) NOT NULL COMMENT '业务类型',
  `business_name` varchar(200) NOT NULL COMMENT '业务名称',
  `price_unit` varchar(50) NOT NULL COMMENT '计价单位',
  `dr_price` decimal(10,4) NOT NULL COMMENT 'DR积分单价',
  `billing_type` int(1) NOT NULL COMMENT '结算类型（1秒结秒扣 2日结日扣）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`price_id`),
  UNIQUE KEY `uk_business_type` (`business_type`),
  KEY `idx_status` (`status`),
  KEY `idx_billing_type` (`billing_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='DR价格配置表';

-- ----------------------------
-- Records of dr_price_config
-- ----------------------------
INSERT INTO `dr_price_config` VALUES
(1, 'INSTANCE_PRE_DEDUCT', '营销实例预扣费', 'DR/个', 100.0000, 1, '0', NOW(), NOW(), 'admin', 'admin', '营销实例创建时预扣费用'),
(2, 'INSTANCE_MARKETING', '营销实例', 'DR/天', 6.0000, 2, '0', NOW(), NOW(), 'admin', 'admin', '营销实例日常运行费用'),
(3, 'INSTANCE_PROSPECTING', '拓客实例', 'DR/天', 1.0000, 2, '0', NOW(), NOW(), 'admin', 'admin', '拓客实例日常运行费用'),
(4, 'SMS', '短信服务', 'DR/条', 0.0500, 1, '0', NOW(), NOW(), 'admin', 'admin', '短信发送费用'),
(5, 'TOKEN', 'AI服务', 'DR/token', 0.0001, 1, '0', NOW(), NOW(), 'admin', 'admin', 'AI服务Token消耗费用');