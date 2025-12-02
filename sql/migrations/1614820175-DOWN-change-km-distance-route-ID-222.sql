-- 1614820175 DOWN change-km-distance-route-ID-222

update config_destination set distance_km = 163 where terminal_origin_id = 6 and terminal_destiny_id = 5 and config_route_id = (select id from config_route where name = '2021 LMM - MZT 5AM L A V' and status= 1);
update config_destination set distance_km = 163 where terminal_origin_id = 5 and terminal_destiny_id = 4 and config_route_id = (select id from config_route where name = '2021 LMM - MZT 5AM L A V' and status= 1);
update config_destination set distance_km = 124 where terminal_origin_id = 1  and terminal_destiny_id = 5 and config_route_id = (select id from config_route where name = '2021 LMM - MZT 5AM L A V' and status= 1);
update config_destination set distance_km = 287 where terminal_origin_id = 1 and terminal_destiny_id = 4 and config_route_id = (select id from config_route where name = '2021 LMM - MZT 5AM L A V' and status= 1);
update config_destination set distance_km = 119 where terminal_origin_id = 8 and terminal_destiny_id = 5 and config_route_id = (select id from config_route where name = '2021 LMM - MZT 5AM L A V' and status= 1);
update config_destination set distance_km = 282 where terminal_origin_id = 8 and terminal_destiny_id = 4 and config_route_id = (select id from config_route where name = '2021 LMM - MZT 5AM L A V' and status= 1);
update config_destination set distance_km = 225 where terminal_origin_id = 6 and terminal_destiny_id = 4 and config_route_id = (select id from config_route where name = '2021 LMM - MZT 5AM L A V' and status= 1);