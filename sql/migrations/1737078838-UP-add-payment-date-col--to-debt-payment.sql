-- 1737078838 UP add-payment-date-col--to-debt-payment
ALTER TABLE debt_payment
ADD COLUMN payment_date DATETIME NULL,
ADD INDEX idx_debt_payment_payment_date (payment_date);