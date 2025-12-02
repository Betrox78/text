-- 1574992706 DOWN add column code in schedule route table
ALTER TABLE schedule_route
DROP INDEX schedule_route_code_idx,
DROP COLUMN code;