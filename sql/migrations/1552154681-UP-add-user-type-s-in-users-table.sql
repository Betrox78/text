-- 1552154681 UP add user type s in users table
ALTER TABLE users
MODIFY COLUMN user_type enum('A', 'C', 'S') NOT NULL DEFAULT 'A';