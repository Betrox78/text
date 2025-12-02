-- 1691691265 UP add-seller-to-customer
ALTER TABLE customer ADD COLUMN seller_id int(11) DEFAULT NULL;
ALTER TABLE customer
ADD CONSTRAINT `fk_customer_seller_id`
FOREIGN KEY (seller_id) REFERENCES `seller` (`id`);