-- 1696749400 UP fcm_token_datetime_change
ALTER TABLE fcm_token MODIFY created_at DATETIME DEFAULT CURRENT_TIMESTAMP;