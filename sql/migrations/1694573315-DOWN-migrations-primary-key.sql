-- 1694573315 DOWN migrations-primary-key
ALTER TABLE migrations MODIFY id INT NOT NULL;
ALTER TABLE migrations DROP PRIMARY KEY;