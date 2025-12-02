-- 1604977399 DOWN cancel-schedule-routes-destinations

update boarding_pass_route set schedule_route_destination_id = 61296 where boarding_pass_id = (select id from boarding_pass where reservation_code = 'B20118E0191');

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
             AND sr.status = 1   ) as temp;


update schedule_route_destination set destination_status = 'scheduled' where id  in (select id from temp_table_schedule_route_id);

drop table temp_table_schedule_route_id;
