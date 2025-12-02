-- 1694152615 DOWN change-wallet-columns
ALTER TABLE `e_wallet_move`
CHANGE COLUMN `service_type` `service_type` ENUM('boarding_pass', 'rental', 'parcel', 'freight', 'guia_pp', 'prepaid') NOT NULL ;

ALTER TABLE `e_wallet_move`
ADD COLUMN e_wallet_recharge_id INT DEFAULT NULL;

ALTER TABLE `e_wallet_move` ADD CONSTRAINT fk_e_wallet_move_e_wallet_recharge_id FOREIGN KEY (e_wallet_recharge_id)
REFERENCES e_wallet_recharge (id);

ALTER TABLE `e_wallet_recharge`
DROP COLUMN conekta_order_id;

ALTER TABLE `e_wallet_recharge`
DROP FOREIGN KEY fk_e_wallet_recharge_e_wallet_recharge_range_id;

ALTER TABLE `e_wallet_recharge`
DROP COLUMN e_wallet_recharges_range_id;