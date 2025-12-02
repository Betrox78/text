-- 1737078838 DOWN add-payment-date-col--to-debt-payment
ALTER TABLE debt_payment
DROP INDEX idx_debt_payment_payment_date,
DROP COLUMN payment_date;