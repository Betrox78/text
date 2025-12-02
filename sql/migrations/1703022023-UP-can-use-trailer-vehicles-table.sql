-- 1703022023 UP can use trailer vehicles table
ALTER TABLE vehicle
ADD COLUMN can_use_trailer BOOLEAN DEFAULT FALSE AFTER last_maintenance;