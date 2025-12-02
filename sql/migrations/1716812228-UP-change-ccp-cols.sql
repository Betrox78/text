-- 1716812228 UP change-ccp-cols
ALTER TABLE travel_logs
ADD COLUMN has_stamp tinyint(1) NOT NULL DEFAULT '0',
ADD COLUMN is_stamped tinyint(1) NOT NULL DEFAULT '0';

ALTER TABLE travel_logs_ccp
ADD COLUMN ccp_type enum('courier', 'freight', 'generic') NOT NULL DEFAULT 'generic',
ADD COLUMN customer_id int(11) DEFAULT NULL,
ADD CONSTRAINT travel_logs_ccp_customer_id_fk FOREIGN KEY (customer_id) REFERENCES customer(id);
