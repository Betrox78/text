-- 1562819052 UP add-service-name-to-invoice
ALTER TABLE invoice
ADD COLUMN service_type ENUM('boarding_pass', 'rental', 'parcel', 'freight') NOT NULL;