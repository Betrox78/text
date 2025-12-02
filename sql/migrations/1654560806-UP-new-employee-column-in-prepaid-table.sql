-- 1654560806 UP new-employee-column-in-prepaid-table
alter table parcels_prepaid
add column `employee_id` int(11) DEFAULT NULL,
add KEY fk_parcels_prepaid_employee_id (employee_id),
add CONSTRAINT fk_parcels_prepaid_employee_id FOREIGN KEY (employee_id) REFERENCES employee (id);