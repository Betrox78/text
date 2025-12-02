-- 1557183143 UP add employee id column in customer table
ALTER TABLE customer
ADD COLUMN employee_id int(11) DEFAULT NULL AFTER is_verified,
ADD CONSTRAINT customer_employee_id_idx FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE SET NULL ON UPDATE CASCADE;