-- 1604977399 UP cancel-schedule-routes-destinations

-- 1604972925 UP guy

update boarding_pass_route set schedule_route_destination_id = 62883 where boarding_pass_id = (select id from boarding_pass where reservation_code = 'B20118E0191');

create temporary table temp_available_ticket
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
 where srd.travel_date BETWEEN '2020-11-10 00:00:00' AND '2020-12-31 23:59:59'
 AND srd.id in ( SELECT  srd.id as id
FROM schedule_route_destination AS srd
                INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id
                INNER JOIN config_route AS cr ON cr.id = sr.config_route_id
                INNER JOIN branchoffice AS bo ON bo.id = srd.terminal_origin_id
                INNER JOIN branchoffice AS bd ON bd.id = srd.terminal_destiny_id
                INNER JOIN vehicle AS v ON v.id = sr.vehicle_id
             WHERE srd.travel_date BETWEEN '2020-11-10 00:00:00' AND '2020-12-31 23:59:59'
              AND srd.terminal_origin_id = cr.terminal_origin_id
            AND srd.terminal_destiny_id = cr.terminal_destiny_id
AND srd.status = 1
             AND sr.status = 1 AND sr.schedule_status = 'canceled'  AND srd.status = 1
             AND sr.status = 1  ) ) AS bps
 GROUP BY bps.schedule_route_id;

create temporary table temp_only_id
select id_srd from (  select id_srd from temp_available_ticket where  seatings > available_seatings  ) as tem;

create temporary table temp_table_schedule_route_id
select id from (SELECT  srd.id as id
FROM schedule_route_destination AS srd
                INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id
                INNER JOIN config_route AS cr ON cr.id = sr.config_route_id
                INNER JOIN branchoffice AS bo ON bo.id = srd.terminal_origin_id
                INNER JOIN branchoffice AS bd ON bd.id = srd.terminal_destiny_id
                INNER JOIN vehicle AS v ON v.id = sr.vehicle_id
             WHERE srd.travel_date BETWEEN '2020-11-09 00:00:00' AND '2020-12-31 23:59:59'
              AND srd.terminal_origin_id = cr.terminal_origin_id
            AND srd.terminal_destiny_id = cr.terminal_destiny_id
AND srd.status = 1
             AND sr.status = 1 AND sr.schedule_status = 'canceled'  AND srd.status = 1
             AND sr.status = 1 AND srd.id not in (select id_srd from temp_only_id) ) as temp;


update schedule_route_destination set destination_status = 'canceled' where id  in (select id from temp_table_schedule_route_id);

drop table temp_table_schedule_route_id;

drop table temp_only_id;

drop table temp_available_ticket;