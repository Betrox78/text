-- 1720487735 DOWN alter table customers default phone email
ALTER TABLE customer
MODIFY COLUMN last_name VARCHAR(50) NOT NULL,
MODIFY COLUMN phone VARCHAR(13) NOT NULL,
MODIFY COLUMN gender ENUM('m', 'f', 'n') NOT NULL DEFAULT 'n';