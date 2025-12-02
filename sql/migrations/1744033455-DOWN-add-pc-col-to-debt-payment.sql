-- 1744033455 DOWN add-pc-col-to-debt-payment
ALTER TABLE debt_payment
DROP FOREIGN KEY `fk_debt_payment_payment_complement_id`;

ALTER TABLE debt_payment
DROP COLUMN payment_complement_id;