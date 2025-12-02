-- 1567207914 DOWN new-fields-for-invoices
ALTER TABLE invoice
DROP COLUMN zip_code,
DROP COLUMN payment_method,
DROP COLUMN cfdi_use;