-- 1694334656 UP additionals-wallet
ALTER TABLE `e_wallet_move`
CHANGE COLUMN `service_type` `service_type` ENUM('boarding_pass', 'rental', 'parcel', 'freight', 'guia_pp', 'prepaid', 'wallet_recharge', 'referral') NOT NULL ;

ALTER TABLE `e_wallet`
DROP FOREIGN KEY fk_e_wallet_referenced_by;

ALTER TABLE `e_wallet`
DROP COLUMN referenced_by;

ALTER TABLE `e_wallet`
ADD COLUMN referenced_by INT DEFAULT NULL;

ALTER TABLE `e_wallet` ADD CONSTRAINT fk_e_wallet_referenced_by FOREIGN KEY (referenced_by)
REFERENCES e_wallet (id);

INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by)
VALUES ('bonus_by_e_wallet_referral', '1.0', 'currency', '1', 'Bonificación a monedero cuando un referido realiza su primer compra', '1', 'Bonificación por referido', 'e-wallet', '1');