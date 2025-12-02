-- 1558206437 UP add state-diff-employee-attendance
ALTER TABLE employee_attendance
ADD COLUMN state_diff INT(11) NOT NULL DEFAULT 0;
