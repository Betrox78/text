-- 1694028676 UP add-wallet-recharge-tickets
ALTER TABLE tickets
ADD COLUMN e_wallet_recharge_id int(11) DEFAULT NULL;

ALTER TABLE tickets ADD CONSTRAINT fk_ticket_e_wallet_recharge_id FOREIGN KEY (e_wallet_recharge_id)
REFERENCES e_wallet_recharge (id);
