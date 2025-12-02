-- 1694025856 DOWN addWalletRechargeToPayments
ALTER TABLE payment
DROP FOREIGN KEY fk_payment_e_wallet_recharge_id;

ALTER TABLE payment
DROP COLUMN e_wallet_recharge_id;