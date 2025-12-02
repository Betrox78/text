-- 1558131890 UP employee-attendance-start-diff
ALTER TABLE employee_attendance
ADD COLUMN start_diff INT(11) NOT NULL DEFAULT 0;