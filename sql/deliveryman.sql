/*
 Navicat Premium Data Transfer

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80020
 Source Host           : localhost:3306
 Source Schema         : food

 Target Server Type    : MySQL
 Target Server Version : 80020
 File Encoding         : 65001

 Date: 12/10/2020 09:35:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for deliveryman
-- ----------------------------
DROP TABLE IF EXISTS `deliveryman`;
CREATE TABLE `deliveryman`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '骑手id',
  `name` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `status` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态',
  `date` datetime(0) NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of deliveryman
-- ----------------------------
INSERT INTO `deliveryman` VALUES (1, 'wangxiaoer', 'AVALIABLE', '2020-06-10 20:30:17');

SET FOREIGN_KEY_CHECKS = 1;
