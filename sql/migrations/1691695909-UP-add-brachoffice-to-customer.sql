-- 1691695909 UP add-brachoffice-to-customer
ALTER TABLE customer ADD COLUMN branchoffice_id int(11) DEFAULT NULL;
ALTER TABLE customer
ADD CONSTRAINT `fk_customer_branchoffice_id`
FOREIGN KEY (branchoffice_id) REFERENCES `branchoffice` (`id`);