-- 1690503135 DOWN customer-employee-parcel-agreement
ALTER TABLE customer
DROP FOREIGN KEY fk_customer_parcels_seller_employee_id;

ALTER TABLE customer
DROP COLUMN parcels_seller_employee_id;

