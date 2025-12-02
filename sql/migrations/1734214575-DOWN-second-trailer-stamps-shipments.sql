-- 1734214575 DOWN second trailer stamps shipments
ALTER TABLE shipments
DROP CONSTRAINT fk_shipments_second_trailer_id,
DROP COLUMN second_trailer_id,
DROP COLUMN second_left_stamp,
DROP COLUMN second_right_stamp,
DROP COLUMN second_additional_stamp,
DROP COLUMN second_replacement_stamp,
DROP COLUMN second_left_stamp_status,
DROP COLUMN second_right_stamp_status,
DROP COLUMN second_additional_stamp_status,
DROP COLUMN second_replacement_stamp_status;