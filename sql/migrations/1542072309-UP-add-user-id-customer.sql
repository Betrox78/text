-- 1542072309 UP add-user-id-customer
ALTER TABLE customer
add column user_id int(11) DEFAULT NULL;