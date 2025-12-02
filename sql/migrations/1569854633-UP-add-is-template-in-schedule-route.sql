-- 1569854633 UP add-is-template-in-schedule-route
ALTER TABLE schedule_route
ADD COLUMN is_template boolean DEFAULT false;
