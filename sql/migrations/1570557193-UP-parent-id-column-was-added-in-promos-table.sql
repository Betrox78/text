-- 1570557193 UP parent id column was added in promos table
ALTER TABLE promos
ADD COLUMN parent_id INT(11) DEFAULT NULL AFTER status,
ADD INDEX parent_id_idx(parent_id);