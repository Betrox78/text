-- 1727134715 UP add-multiple-column-to-invoice
ALTER TABLE invoice
ADD COLUMN is_multiple BOOLEAN NOT NULL DEFAULT FALSE AFTER is_global;