-- 1694028676 DOWN add-wallet-recharge-tickets
ALTER TABLE tickets
DROP FOREIGN KEY fk_ticket_e_wallet_recharge_id;

ALTER TABLE tickets
DROP COLUMN e_wallet_recharge_id;