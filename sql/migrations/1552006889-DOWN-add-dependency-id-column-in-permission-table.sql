-- 1552006889 DOWN add dependency id column in permission table
ALTER TABLE permission
DROP COLUMN dependency_id;