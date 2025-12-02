-- 1556842755 DOWN set null profile id field in users table
ALTER TABLE users
MODIFY COLUMN profile_id int(11) NOT NULL,
MODIFY COLUMN created_by int(11) NOT NULL;