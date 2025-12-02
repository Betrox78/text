-- 1544559118 DOWN add-columns-for-payback-info-in-tickets-table
ALTER TABLE tickets
DROP COLUMN payback_before,
DROP COLUMN payback_money;