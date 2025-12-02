-- 1653609451 DOWN alter-column-service-type-invoice
ALTER TABLE invoice
MODIFY COLUMN service_type enum('boarding_pass','rental','parcel','freight') NOT NULL;