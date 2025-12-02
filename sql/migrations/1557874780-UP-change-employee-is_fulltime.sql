-- 1557874780 UP change-employee-is_fulltime

ALTER TABLE employee
MODIFY COLUMN is_fulltime TINYINT(4) DEFAULT 1;