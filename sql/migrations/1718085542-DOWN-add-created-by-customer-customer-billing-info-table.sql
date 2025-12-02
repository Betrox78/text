-- 1718085542 DOWN add created by customer customer billing info table
ALTER TABLE customer_customer_billing_info
DROP CONSTRAINT fk_customer_customer_billing_info_created_by,
DROP COLUMN created_by;