-- 1570804066 UP add-debt-on-boarding-pass
ALTER TABLE boarding_pass
    ADD COLUMN debt FLOAT(12,2) DEFAULT 0 AFTER conekta_order_id;