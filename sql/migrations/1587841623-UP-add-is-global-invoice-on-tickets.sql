-- 1587841623 UP add-is-global-invoice-on-tickets
ALTER TABLE tickets
ADD COLUMN is_global_invoice boolean DEFAULT false;