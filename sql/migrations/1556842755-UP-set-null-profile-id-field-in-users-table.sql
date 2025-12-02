-- 1556842755 UP set null profile id field in users table
ALTER TABLE users
MODIFY COLUMN profile_id int(11) DEFAULT NULL,
MODIFY COLUMN created_by int(11) DEFAULT NULL;