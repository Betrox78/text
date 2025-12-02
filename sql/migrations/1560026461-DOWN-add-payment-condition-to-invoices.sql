-- 1560026461 DOWN add-payment-condition-to-invoices
ALTER TABLE invoice
DROP COLUMN payment_condition;