-- 1694152615 UP change-wallet-columns
ALTER TABLE `e_wallet_move`
CHANGE COLUMN `service_type` `service_type` ENUM('boarding_pass', 'rental', 'parcel', 'freight', 'guia_pp', 'prepaid', 'wallet_recharge') NOT NULL ;

ALTER TABLE `e_wallet_move`
DROP FOREIGN KEY fk_e_wallet_move_e_wallet_recharge_id;

ALTER TABLE `e_wallet_move`
DROP COLUMN e_wallet_recharge_id;

ALTER TABLE `e_wallet_recharge`
ADD COLUMN `conekta_order_id` VARCHAR(25) NULL DEFAULT NULL AFTER `promo_id`;

ALTER TABLE `e_wallet_recharge`
ADD COLUMN e_wallet_recharges_range_id INT DEFAULT NULL;

ALTER TABLE `e_wallet_recharge` ADD CONSTRAINT fk_e_wallet_recharge_e_wallet_recharge_range_id FOREIGN KEY (e_wallet_recharges_range_id)
REFERENCES e_wallet_recharges_range (id);