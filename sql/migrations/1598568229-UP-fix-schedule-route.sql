-- 1598568229 UP fix-schedule-route
update schedule_route_destination set destination_status='scheduled' where id IN (54877,54878);
