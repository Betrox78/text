-- 1566495387 DOWN shedule-route-destinationid-can-be-null-in-boardingpassroute
ALTER TABLE boarding_pass_route
MODIFY schedule_route_destination_id INT(11) NOT NULL;