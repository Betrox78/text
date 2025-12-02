-- 1734214575 UP second trailer stamps shipments
ALTER TABLE shipments
ADD COLUMN second_trailer_id INTEGER DEFAULT NULL AFTER replacement_stamp_status,
ADD COLUMN second_left_stamp varchar(100) DEFAULT NULL AFTER second_trailer_id,
ADD COLUMN second_right_stamp varchar(100) DEFAULT NULL AFTER second_left_stamp,
ADD COLUMN second_additional_stamp varchar(25) DEFAULT NULL AFTER second_right_stamp,
ADD COLUMN second_replacement_stamp varchar(25) DEFAULT NULL AFTER second_additional_stamp,
ADD COLUMN second_left_stamp_status int NOT NULL DEFAULT '1' AFTER second_replacement_stamp,
ADD COLUMN second_right_stamp_status int NOT NULL DEFAULT '1' AFTER second_left_stamp_status,
ADD COLUMN second_additional_stamp_status int DEFAULT '1' AFTER second_right_stamp_status,
ADD COLUMN second_replacement_stamp_status int DEFAULT '1' AFTER second_additional_stamp_status,
ADD CONSTRAINT fk_shipments_second_trailer_id FOREIGN KEY (second_trailer_id) REFERENCES trailers(id);