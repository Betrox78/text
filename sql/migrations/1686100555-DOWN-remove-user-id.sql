ALTER TABLE prepaid_package_travel DROP FOREIGN KEY fk_prepaid_seller_user_id;
ALTER TABLE prepaid_package_travel DROP COLUMN seller_user_id;

ALTER TABLE prepaid_package_travel ADD COLUMN employee_id int(11) DEFAULT NULL;
ALTER TABLE prepaid_package_travel
ADD CONSTRAINT `fk_prepaid_employee_id`
FOREIGN KEY (employee_id) REFERENCES `employee` (`id`);