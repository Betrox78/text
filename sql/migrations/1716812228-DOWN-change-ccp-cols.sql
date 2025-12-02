-- 1716812228 DOWN change-ccp-cols
ALTER TABLE travel_logs
DROP COLUMN has_stamp,
DROP COLUMN is_stamped;

ALTER TABLE travel_logs_ccp
DROP FOREIGN KEY travel_logs_ccp_customer_id_fk,
DROP COLUMN ccp_type,
DROP COLUMN customer_id;