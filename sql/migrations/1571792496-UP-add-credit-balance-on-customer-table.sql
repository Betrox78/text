-- 1571792496 UP add-credit-balance-on-customer-table
ALTER TABLE customer
    ADD COLUMN credit_balance FLOAT(12,2) DEFAULT 0;