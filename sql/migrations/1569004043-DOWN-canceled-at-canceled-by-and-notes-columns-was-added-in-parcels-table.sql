-- 1569004043 DOWN canceled at canceled by and notes columns was added in parcels table
ALTER TABLE parcels
DROP COLUMN notes,
DROP COLUMN canceled_at,
DROP COLUMN canceled_by;