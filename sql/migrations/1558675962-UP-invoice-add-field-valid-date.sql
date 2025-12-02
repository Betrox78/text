-- 1558675962 UP invoice-add-field-valid-date
ALTER TABLE invoice
ADD COLUMN init_valid_at DATETIME NOT NULL;