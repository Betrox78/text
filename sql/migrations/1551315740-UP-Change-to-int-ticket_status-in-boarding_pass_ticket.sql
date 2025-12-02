-- 1551315740 UP Change to int ticket_status in boarding_pass_ticket
ALTER TABLE boarding_pass_ticket 
CHANGE COLUMN ticket_status ticket_status INT(11) NULL DEFAULT 1 ;
