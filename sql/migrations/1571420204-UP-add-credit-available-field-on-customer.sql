-- 1571420204 UP add-credit-available-field-on-customer
ALTER TABLE customer
    ADD COLUMN credit_available FLOAT(12,2) DEFAULT 0;