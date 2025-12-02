-- 1553841961 UP add-email-to-invoice
ALTER TABLE invoice
ADD COLUMN email VARCHAR(100) NOT NULL;