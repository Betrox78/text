-- 1552154681 DOWN add user type s in users table
ALTER TABLE users
MODIFY COLUMN user_type enum('A', 'C') NOT NULL DEFAULT 'A';