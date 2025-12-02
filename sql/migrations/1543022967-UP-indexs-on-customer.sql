-- 1543022967 UP indexs-on-customer
ALTER TABLE customer
MODIFY token varchar(128) DEFAULT NULL;

CREATE INDEX index_customer_email ON customer(email);

CREATE UNIQUE INDEX index_customer_token ON customer(token);