-- 1574992706 UP add column code in schedule route table
ALTER TABLE schedule_route
ADD COLUMN code VARCHAR(50) DEFAULT NULL AFTER schedule_status,
ADD UNIQUE INDEX schedule_route_code_idx(code);