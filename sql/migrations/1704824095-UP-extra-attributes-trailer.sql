-- 1704824095 UP extra attributes trailer
ALTER TABLE trailers
ADD COLUMN economic_number VARCHAR(20) DEFAULT NULL AFTER name,
ADD COLUMN serial_number VARCHAR(100) DEFAULT NULL AFTER economic_number;