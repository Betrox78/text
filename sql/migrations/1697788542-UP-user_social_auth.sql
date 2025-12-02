-- 1697788542 UP user_social_auth
ALTER TABLE users ADD social_auth TINYINT(1) NOT NULL DEFAULT 0;