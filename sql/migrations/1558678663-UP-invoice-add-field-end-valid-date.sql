-- 1558678663 UP invoice-add-field-end-valid-date
ALTER TABLE invoice
ADD COLUMN end_valid_at DATETIME NOT NULL;