-- 1703707972 UP add-ccp-travel-log-id-to-travel-logs
ALTER TABLE travel_logs ADD COLUMN travel_logs_ccp_id int(11) DEFAULT NULL;
ALTER TABLE travel_logs
ADD CONSTRAINT `fk_travel_logs_travel_logs_ccp_id`
FOREIGN KEY (travel_logs_ccp_id) REFERENCES `travel_logs_ccp` (`id`);