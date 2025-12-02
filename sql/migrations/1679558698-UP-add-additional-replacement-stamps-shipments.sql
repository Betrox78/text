-- 1679558698 UP add additional replacement stamps shipments
ALTER TABLE shipments
ADD COLUMN additional_stamp VARCHAR(25) DEFAULT NULL AFTER right_stamp,
ADD COLUMN replacement_stamp VARCHAR(25) DEFAULT NULL AFTER additional_stamp,
ADD COLUMN additional_stamp_status INT DEFAULT 1 AFTER right_stamp_status,
ADD COLUMN replacement_stamp_status INT DEFAULT 1 AFTER additional_stamp_status;