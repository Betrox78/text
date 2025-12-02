-- 1552006889 UP add dependency id column in permission table
ALTER TABLE permission
ADD COLUMN dependency_id int(11) DEFAULT NULL AFTER description;