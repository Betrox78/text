-- 1568919824 UP add-time-checkpoint-in-schedule-route-destination
ALTER TABLE schedule_route_destination
ADD COLUMN time_checkpoint int(11) DEFAULT NULL