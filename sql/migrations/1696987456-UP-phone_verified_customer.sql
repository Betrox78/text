-- 1696987456 UP phone_verified_customer
ALTER TABLE customer ADD is_phone_verified TINYINT(1) NOT NULL DEFAULT 0;