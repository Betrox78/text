-- 1546104306 DOWN modify-noext-addresses
ALTER TABLE customer_addresses
MODIFY COLUMN no_ext int(11) DEFAULT NULL;