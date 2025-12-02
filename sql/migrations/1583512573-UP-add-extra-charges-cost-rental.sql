-- 1583512573 UP add-extra-charges-cost-rental
ALTER TABLE rental
    ADD COLUMN cost FLOAT(12,2) DEFAULT 0;