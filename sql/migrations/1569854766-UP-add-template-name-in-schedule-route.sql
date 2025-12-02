-- 1569854766 UP add-template-name-in-schedule-route
ALTER TABLE schedule_route
ADD COLUMN template_name varchar(100);