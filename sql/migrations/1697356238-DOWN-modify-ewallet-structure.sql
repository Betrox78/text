-- 1697356238 DOWN modify-ewallet-structure
ALTER TABLE `e_wallet`
DROP COLUMN `available_bonus`;

ALTER TABLE `e_wallet_recharge`
ADD COLUMN `recharge_type` enum('purchase','promotion', 'bonus') NOT NULL DEFAULT 'purchase',
DROP COLUMN `code`,
DROP COLUMN `bonification`;

ALTER TABLE `e_wallet_move`
DROP COLUMN `wallet_type`,
DROP COLUMN `before_amount`,
DROP COLUMN `after_amount`;