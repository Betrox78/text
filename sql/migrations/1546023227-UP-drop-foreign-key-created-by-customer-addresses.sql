-- 1546023227 UP drop-foreign-key-created-by-customer-addresses
ALTER TABLE customer_addresses
DROP KEY customer_addresses_created_by_idx,
DROP FOREIGN KEY customer_addresses_created_by;