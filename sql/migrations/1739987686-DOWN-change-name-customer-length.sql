-- 1739987686 DOWN change-name-customer-length
ALTER TABLE `customer`
CHANGE COLUMN `first_name` `first_name` VARCHAR(50) NOT NULL;