-- 1694025856 UP addWalletRechargeToPayments
ALTER TABLE payment
ADD COLUMN e_wallet_recharge_id int(11) DEFAULT NULL;

ALTER TABLE payment ADD CONSTRAINT fk_payment_e_wallet_recharge_id FOREIGN KEY (e_wallet_recharge_id)
REFERENCES e_wallet_recharge (id);