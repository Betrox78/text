-- 1703707972 DOWN add-ccp-travel-log-id-to-travel-logs
ALTER TABLE travel_logs
DROP FOREIGN KEY fk_travel_logs_travel_logs_ccp_id;

ALTER TABLE travel_logs
DROP COLUMN travel_logs_ccp_id;