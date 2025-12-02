-- 1606840635 UP add-column-status_hide
ALTER TABLE schedule_route
ADD COLUMN status_hide tinyint(4) NOT NULL DEFAULT 0;