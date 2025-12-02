-- 1679558698 DOWN add additional replacement stamps shipments
ALTER TABLE shipments
DROP COLUMN additional_stamp,
DROP COLUMN replacement_stamp,
DROP COLUMN additional_stamp_status,
DROP COLUMN replacement_stamp_status;