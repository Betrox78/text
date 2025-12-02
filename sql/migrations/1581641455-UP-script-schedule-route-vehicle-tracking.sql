-- 1581641455 UP script-schedule-route-vehicle-tracking
INSERT INTO schedule_route_vehicle_tracking(schedule_route_id, vehicle_id, action, created_at, created_by)
(SELECT id, vehicle_id, 'assigned', created_at, COALESCE(created_by, 1) FROM schedule_route WHERE status = 1);