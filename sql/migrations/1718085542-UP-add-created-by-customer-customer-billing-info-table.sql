-- 1718085542 UP add created by customer customer billing info table
ALTER TABLE customer_customer_billing_info
ADD COLUMN created_by INTEGER(11) NOT NULL AFTER created_at,
ADD CONSTRAINT fk_customer_customer_billing_info_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE NO ACTION;