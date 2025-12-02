-- 1653609451 UP alter-column-service-type-invoice
ALTER TABLE invoice
MODIFY COLUMN service_type enum('boarding_pass','rental','parcel','freight', 'guia_pp') NOT NULL;