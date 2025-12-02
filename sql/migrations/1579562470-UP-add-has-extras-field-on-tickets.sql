-- 1579562470 UP add-has-extras-field-on-tickets
ALTER TABLE tickets
    ADD COLUMN has_extras boolean DEFAULT false;