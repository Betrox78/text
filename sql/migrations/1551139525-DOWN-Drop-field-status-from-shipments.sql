-- 1551139525 DOWN Drop field status from shipments
ALTER TABLE shipments 
ADD COLUMN status INT(11) NOT NULL DEFAULT 1 AFTER total_packages;