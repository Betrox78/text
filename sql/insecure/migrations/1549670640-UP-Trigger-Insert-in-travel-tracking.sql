-- 1549670640 UP Trigger Insert in travel-tracking
DROP TRIGGER IF EXISTS `schedule_route_destination_AFTER_UPDATE`;
DELIMITER $$
CREATE DEFINER = CURRENT_USER TRIGGER `schedule_route_destination_AFTER_UPDATE` AFTER UPDATE ON `schedule_route_destination` FOR EACH ROW
BEGIN
	INSERT 
		INTO travel_tracking (
			schedule_route_id,
            schedule_route_destination_id,
            status,
            created_by) 
        VALUES (
			NEW.schedule_route_id,
            NEW.id,
            NEW.destination_status,
            NEW.updated_by);
END$$
DELIMITER ;