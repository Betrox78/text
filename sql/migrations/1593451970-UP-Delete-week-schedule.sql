-- 1593451970 UP Delete-week-schedule
update schedule_route_destination set destination_status='canceled'
where schedule_route_id IN (select id from schedule_route where date(travel_date) between '2020-07-01' and '2020-07-05');

update schedule_route set schedule_status='canceled' where date(travel_date) between '2020-07-01' and '2020-07-05';
