-- 1557183143 DOWN add employee id column in customer table
ALTER TABLE customer
DROP FOREIGN KEY customer_employee_id_idx,
DROP COLUMN employee_id;