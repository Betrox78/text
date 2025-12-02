-- 1605031777 DOWN cancel-between-stops

drop table if exists temp_available_ticket;

create temporary table temp_available_ticket
 SELECT
 distinct srd.id AS id
 FROM schedule_route_destination AS srd
 INNER JOIN schedule_route AS sr
 ON sr.id=srd.schedule_route_id
 INNER JOIN vehicle AS vh
 ON vh.id=sr.vehicle_id
 INNER JOIN config_vehicle AS cv
 ON cv.id=vh.config_vehicle_id
 INNER JOIN config_destination AS cd
 ON cd.id=srd.config_destination_id
 INNER JOIN config_route AS cr
 ON cr.id=cd.config_route_id
 WHERE
	srd.travel_date BETWEEN '2020-11-11 17:19:28' AND '2020-12-31 23:59:59'
 AND srd.status = 1 AND (srd.destination_status = 'canceled')
 AND sr.status = 1;

update schedule_route_destination set destination_status = 'scheduled' where id  in (select id from temp_available_ticket);



drop table if exists temp_available_ticket;