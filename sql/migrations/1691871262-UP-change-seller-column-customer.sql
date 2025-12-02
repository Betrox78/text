-- 1691871262 UP change-seller-column-customer
ALTER TABLE customer DROP FOREIGN KEY fk_customer_seller_id;
ALTER TABLE customer DROP COLUMN seller_id;

ALTER TABLE customer ADD COLUMN user_seller_id int(11) DEFAULT NULL;
ALTER TABLE customer
ADD CONSTRAINT `fk_customer_user_seller_id`
FOREIGN KEY (user_seller_id) REFERENCES `users` (`id`);