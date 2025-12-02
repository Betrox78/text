-- 1720487735 UP alter table customers default phone email
ALTER TABLE customer
MODIFY COLUMN last_name VARCHAR(50) DEFAULT '',
MODIFY COLUMN phone VARCHAR(13) DEFAULT '',
MODIFY COLUMN gender ENUM('m', 'f', 'n') NOT NULL DEFAULT 'n';