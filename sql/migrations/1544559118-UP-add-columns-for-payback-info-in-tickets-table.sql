-- 1544559118 UP add-columns-for-payback-info-in-tickets-table
ALTER TABLE tickets
ADD COLUMN payback_before decimal(12,2) NOT NULL DEFAULT 0.0 AFTER paid_change,
ADD COLUMN payback_money decimal(12,2) NOT NULL DEFAULT 0.0 AFTER payback_before;