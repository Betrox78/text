-- 1697050972 UP user_phone_verified_change
ALTER TABLE users ADD is_phone_verified TINYINT(1) NOT NULL DEFAULT 0;