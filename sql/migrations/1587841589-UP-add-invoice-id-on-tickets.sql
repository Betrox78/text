-- 1587841589 UP add-invoice-id-on-tickets
ALTER TABLE tickets
ADD COLUMN invoice_id INT(11) DEFAULT NULL