-- 1697356238 UP modify-ewallet-structure
ALTER TABLE `e_wallet`
ADD COLUMN `available_bonus` DECIMAL(12,2) NOT NULL DEFAULT '0.00' AFTER `available_amount`;

ALTER TABLE `e_wallet_recharge`
DROP COLUMN `recharge_type`,
ADD COLUMN `code` VARCHAR(60) NULL DEFAULT NULL AFTER `e_wallet_id`,
ADD COLUMN `bonification` DECIMAL(12,2) NOT NULL DEFAULT '0.00' AFTER `total_amount`;

ALTER TABLE `e_wallet_move`
ADD COLUMN `wallet_type` ENUM('wallet_recharge', 'bonus') NOT NULL AFTER `e_wallet_id`,
ADD COLUMN `before_amount` DECIMAL(12,2) NOT NULL DEFAULT '0.00' AFTER `service_type`,
ADD COLUMN `after_amount` DECIMAL(12,2) NOT NULL DEFAULT '0.00' AFTER `amount`;