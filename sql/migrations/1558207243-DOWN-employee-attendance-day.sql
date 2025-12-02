-- 1558207243 DOWN employee-attendance-day
ALTER TABLE employee
DROP FOREIGN KEY employee_employee_attendance_day_id_fk_idx,
DROP COLUMN employee_attendance_day_id;

DROP TABLE employee_attendance_day;