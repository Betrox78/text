-- 1654560806 DOWN new-employee-column-in-prepaid-table
alter table parcels_prepaid
drop foreign key fk_parcels_prepaid_employee_id,
drop column employee_id