-- 1558193989 UP end-diff-employee-attendance
ALTER TABLE employee_attendance
ADD COLUMN end_diff INT(11) NOT NULL DEFAULT 0;
