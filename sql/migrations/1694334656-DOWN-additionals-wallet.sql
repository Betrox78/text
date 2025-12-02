-- 1694334656 DOWN additionals-wallet
ALTER TABLE `e_wallet_move`
CHANGE COLUMN `service_type` `service_type` ENUM('boarding_pass', 'rental', 'parcel', 'freight', 'guia_pp', 'prepaid', 'wallet_recharge') NOT NULL ;

ALTER TABLE `e_wallet`
DROP FOREIGN KEY fk_e_wallet_referenced_by;

ALTER TABLE `e_wallet`
DROP COLUMN referenced_by;

ALTER TABLE `e_wallet`
ADD COLUMN referenced_by INT DEFAULT NULL;

ALTER TABLE `e_wallet` ADD CONSTRAINT fk_e_wallet_referenced_by FOREIGN KEY (referenced_by)
REFERENCES users (id);

DELETE from general_setting gs where gs.FIELD = 'bonus_by_e_wallet_referral' AND gs.id > 0;