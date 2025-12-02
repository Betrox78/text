-- 1697092349 DOWN fcm_unique_change
ALTER TABLE fcm_token
DROP INDEX customer_token_unique;

ALTER TABLE fcm_token
ADD UNIQUE KEY (token);