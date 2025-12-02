-- 1697092349 UP fcm_unique_change
ALTER TABLE fcm_token
DROP INDEX token;

ALTER TABLE fcm_token
ADD UNIQUE KEY customer_token_unique (customer_id, token);