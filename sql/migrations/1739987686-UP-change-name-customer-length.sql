-- 1739987686 UP change-name-customer-length
ALTER TABLE `customer`
CHANGE COLUMN `first_name` `first_name` VARCHAR(255) NOT NULL;
