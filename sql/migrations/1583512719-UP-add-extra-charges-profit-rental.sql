-- 1583512719 UP add-extra-charges-profit-rental
ALTER TABLE rental
    ADD COLUMN profit FLOAT(12,2) DEFAULT 0;