-- 1598568229 DOWN fix-schedule-route
update schedule_route_destination set destination_status='in-transit' where id IN (54877,54878);
