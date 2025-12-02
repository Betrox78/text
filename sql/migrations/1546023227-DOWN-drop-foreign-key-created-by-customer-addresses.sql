-- 1546023227 DOWN drop-foreign-key-created-by-customer-addresses
ALTER TABLE customer_addresses
ADD KEY customer_addresses_created_by_idx (created_by),
ADD CONSTRAINT customer_addresses_created_by FOREIGN KEY (created_by) REFERENCES employee (id) ON DELETE NO ACTION ON UPDATE NO ACTION;

