-- 1567435338 UP add-config-destination-id-in-boarding-pass-route
ALTER TABLE boarding_pass_route
ADD COLUMN config_destination_id INT(11) NULL;

