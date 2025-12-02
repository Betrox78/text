-- 1691871262 DOWN change-seller-column-customer
ALTER TABLE customer DROP FOREIGN KEY fk_customer_user_seller_id;
ALTER TABLE customer DROP COLUMN user_seller_id;

ALTER TABLE customer ADD COLUMN seller_id int(11) DEFAULT NULL;
ALTER TABLE customer
ADD CONSTRAINT `fk_customer_seller_id`
FOREIGN KEY (seller_id) REFERENCES `seller` (`id`);
