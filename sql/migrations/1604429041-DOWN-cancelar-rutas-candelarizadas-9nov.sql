-- 1604429041 DOWN cancelar-rutas-candelarizadas-9nov
create temporary table temp_canceladas
select id from (SELECT  srd.schedule_route_id as id
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
                             AND sr.status = 1 );


create temporary table temp_table_origin
select id from (SELECT  srd.schedule_route_id as id
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
             AND sr.status = 1  AND sr.id not in ( select id from temp_canceladas)  )as temp ;

            -- select * from temp_table_origin;

 update schedule_route set schedule_status = 'scheduled' where id in (select id from temp_table_origin );


  drop table temp_canceladas;
  drop table temp_table_origin;