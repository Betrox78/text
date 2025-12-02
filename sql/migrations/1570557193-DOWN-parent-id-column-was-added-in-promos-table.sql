-- 1570557193 DOWN parent id column was added in promos table
ALTER TABLE promos
DROP INDEX parent_id_idx,
DROP COLUMN parent_id;