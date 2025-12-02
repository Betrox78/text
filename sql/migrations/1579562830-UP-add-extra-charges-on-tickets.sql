-- 1579562830 UP add-extra-charges-on-tickets
ALTER TABLE tickets
    ADD COLUMN extra_charges FLOAT(12,2) DEFAULT 0;