-- 1560535658 UP modify available days column in promos table
ALTER TABLE promos
MODIFY COLUMN available_days varchar(27) NOT NULL DEFAULT 'all';