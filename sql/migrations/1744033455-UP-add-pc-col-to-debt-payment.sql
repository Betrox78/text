-- 1744033455 UP add-pc-col-to-debt-payment
ALTER TABLE debt_payment
ADD COLUMN payment_complement_id int DEFAULT NULL;

ALTER TABLE debt_payment
ADD CONSTRAINT `fk_debt_payment_payment_complement_id`
FOREIGN KEY (payment_complement_id) REFERENCES `payment_complement` (`id`);