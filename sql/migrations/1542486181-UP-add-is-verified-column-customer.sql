-- 1542486181 UP add-is-verified-column-customer
ALTER TABLE customer
ADD COLUMN is_verified boolean DEFAULT false AFTER is_business;
