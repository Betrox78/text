-- 1550596894 UP Add column tracking_code in boarding_pass_ticket
ALTER TABLE boarding_pass_ticket 
ADD COLUMN tracking_code VARCHAR(50) NULL AFTER printed_at;