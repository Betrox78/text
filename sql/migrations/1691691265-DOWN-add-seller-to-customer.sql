-- 1691691265 DOWN add-seller-to-customer
ALTER TABLE customer DROP FOREIGN KEY fk_customer_seller_id;
ALTER TABLE customer DROP COLUMN seller_id;