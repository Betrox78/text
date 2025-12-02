-- 1737826303 UP add-relate-ccp-to-invoice-on-customer
ALTER TABLE customer
ADD COLUMN relate_ccp_with_invoice BOOLEAN NOT NULL DEFAULT FALSE AFTER contact;