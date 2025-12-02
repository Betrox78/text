-- 1605031777 UP cancel-between-stops
-- 1605031777 UP cancel-between-stops

drop table if exists temp_available_ticket;
drop table if exists boletos_vendidos;
drop table if exists filtro_vendido;
drop table if exists table_final;

create temporary table temp_available_ticket
 SELECT
 distinct srd.schedule_route_id AS schedule_route_id
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
 AND srd.status = 1 AND (srd.destination_status = 'scheduled' OR srd.destination_status = 'loading' OR srd.destination_status = 'ready-to-load')
 AND sr.status = 1;

select * from schedule_route where id in (select schedule_route_id from temp_available_ticket) and schedule_status = 'canceled';

 create temporary table boletos_vendidos
  SELECT
  bps.seatings, bps.seatings - SUM(IF(bps.status IS NOT NULL AND bps.ticket_status IN (1, 2), 1, 0)) AS available_seatings, bps.schedule_route_id , bps.id_srd FROM
  (SELECT cv.seatings, bpt.id, bpt.seat, bpt.ticket_status, bp.status, bp.boardingpass_status, srd.schedule_route_id , srd.id as id_srd
  FROM schedule_route_destination AS srd
  LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id
  LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id
  LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id
  LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id
  LEFT JOIN boarding_pass_route AS bpr ON srd.id=bpr.schedule_route_destination_id
  LEFT JOIN boarding_pass AS bp ON bp.id=bpr.boarding_pass_id AND bp.status = 1 AND bp.boardingpass_status IN (1, 2, 4)
  LEFT JOIN boarding_pass_ticket AS bpt ON bpr.id=bpt.boarding_pass_route_id AND bpt.status = 1 AND bpt.seat != ''
  where srd.travel_date BETWEEN '2020-11-11 00:00:00' AND '2020-12-31 23:59:59'
  AND srd.schedule_route_id in (
  select id from schedule_route where id in (select schedule_route_id from temp_available_ticket) and schedule_status = 'canceled'
  )
 ) AS bps
  GROUP BY bps.schedule_route_id;

  create temporary table filtro_vendido
  select schedule_route_id from (select schedule_route_id from boletos_vendidos where seatings = available_seatings) as tempo1;

  create temporary table table_final
	select distinct srd.id AS id
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
	and srd.schedule_route_id in ( select schedule_route_id from filtro_vendido)
 AND srd.status = 1 AND (srd.destination_status = 'scheduled' OR srd.destination_status = 'loading' OR srd.destination_status = 'ready-to-load')
 AND sr.status = 1;


update schedule_route_destination set destination_status = 'canceled' where id  in (select id from table_final);


drop table if exists temp_available_ticket;
drop table if exists boletos_vendidos;
drop table if exists filtro_vendido;
drop table if exists table_final;