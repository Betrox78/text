-- 1699052163 UP add-company-nickname-to-customers
ALTER TABLE customer
ADD COLUMN `company_nick_name` VARCHAR(255) NULL DEFAULT NULL;