-- 1557963617 UP add parent id column in parcels table
ALTER TABLE parcels
ADD COLUMN parent_id int(11) DEFAULT NULL;