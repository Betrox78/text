-- 1560026461 UP add-payment-condition-to-invoices
ALTER TABLE invoice
ADD COLUMN payment_condition ENUM('credit', 'cash') NOT NULL DEFAULT 'cash';