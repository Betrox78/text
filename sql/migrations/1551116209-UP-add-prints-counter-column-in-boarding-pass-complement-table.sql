-- 1551116209 UP add-prints-counter-column-in-boarding-pass-complement-table
ALTER TABLE boarding_pass_complement
ADD COLUMN prints_counter int(11) NOT NULL DEFAULT 0 COMMENT 'Prints counter' AFTER tracking_code;