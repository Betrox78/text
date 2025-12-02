-- 1567207914 UP new-fields-for-invoices
ALTER TABLE invoice
ADD COLUMN zip_code INT(11) NULL,
ADD COLUMN payment_method VARCHAR(5) NULL,
ADD COLUMN cfdi_use VARCHAR(5) NULL;