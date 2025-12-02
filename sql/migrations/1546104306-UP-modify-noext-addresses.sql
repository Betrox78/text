-- 1546104306 UP modify-noext-addresses
ALTER TABLE customer_addresses
MODIFY COLUMN no_ext varchar(15) DEFAULT NULL;