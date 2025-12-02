-- 1558660901 UP add-is-global-to-invoice
ALTER TABLE invoice
ADD COLUMN is_global BIT(1) NOT NULL DEFAULT 0;