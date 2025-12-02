-- 1691695909 DOWN add-brachoffice-to-customer
ALTER TABLE customer DROP FOREIGN KEY fk_customer_branchoffice_id;
ALTER TABLE customer DROP COLUMN branchoffice_id;