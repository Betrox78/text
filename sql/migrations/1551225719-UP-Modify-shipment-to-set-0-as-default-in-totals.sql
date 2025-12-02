-- 1551225719 UP Modify shipment to set 0 as default in totals

ALTER TABLE shipments 
CHANGE COLUMN total_tickets total_tickets INT(11) NOT NULL DEFAULT 0 ,
CHANGE COLUMN total_complements total_complements INT(11) NOT NULL DEFAULT 0 ,
CHANGE COLUMN total_packages total_packages INT(11) NOT NULL DEFAULT 0 ;