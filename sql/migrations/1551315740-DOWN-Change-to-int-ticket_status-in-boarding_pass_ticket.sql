-- 1551315740 DOWN Change to int ticket_status in boarding_pass_ticket
ALTER TABLE boarding_pass_ticket 
CHANGE COLUMN ticket_status ticket_status TINYINT(1) NULL DEFAULT '1' ;
