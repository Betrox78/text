ALTER TABLE prepaid_package_travel DROP FOREIGN KEY fk_prepaid_employee_id;
ALTER TABLE prepaid_package_travel DROP COLUMN employee_id;

ALTER TABLE prepaid_package_travel ADD COLUMN seller_user_id int(11) DEFAULT NULL;
ALTER TABLE prepaid_package_travel
ADD CONSTRAINT `fk_prepaid_seller_user_id`
FOREIGN KEY (seller_user_id) REFERENCES `users` (`id`);