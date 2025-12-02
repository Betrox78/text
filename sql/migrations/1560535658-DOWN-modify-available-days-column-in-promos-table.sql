-- 1560535658 DOWN modify available days column in promos table
ALTER TABLE promos
MODIFY COLUMN available_days varchar(3) NOT NULL DEFAULT 'all';