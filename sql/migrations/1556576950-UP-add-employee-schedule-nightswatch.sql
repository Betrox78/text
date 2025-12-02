-- 1556576950 UP add-employee-schedule-nightswatch
ALTER TABLE `employee_schedule`
ADD COLUMN `nights_watch` TINYINT(1) NULL DEFAULT 0;
