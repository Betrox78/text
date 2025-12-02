-- 1566495387 UP shedule-route-destinationid-can-be-null-in-boardingpassroute
ALTER TABLE boarding_pass_route
MODIFY schedule_route_destination_id INT(11) NULL;