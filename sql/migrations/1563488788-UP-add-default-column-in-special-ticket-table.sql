-- 1563488788 UP add default column in special ticket table
ALTER TABLE special_ticket
ADD COLUMN is_default tinyint(1) NOT NULL DEFAULT 0 AFTER description;