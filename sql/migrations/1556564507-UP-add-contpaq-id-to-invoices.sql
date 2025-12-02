-- 1556564507 UP add-contpaq-id-to-invoices
ALTER TABLE invoice
ADD COLUMN contpaq_id INT(11) DEFAULT NULL;