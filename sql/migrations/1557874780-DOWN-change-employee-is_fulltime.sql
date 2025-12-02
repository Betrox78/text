-- 1557874780 DOWN change-employee-is_fulltime

ALTER TABLE employee
MODIFY COLUMN is_fulltime TINYINT(1) DEFAULT 1;