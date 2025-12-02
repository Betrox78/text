-- 1729821668 UP add-invoice-email-to-customer
ALTER TABLE customer
ADD COLUMN invoice_email VARCHAR(100) NULL DEFAULT NULL AFTER email;