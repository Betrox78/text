-- 1543022967 DOWN indexs-on-customer
ALTER TABLE customer
MODIFY token text DEFAULT NULL;

DROP INDEX index_customer_email ON customer;

DROP INDEX index_customer_token ON customer;