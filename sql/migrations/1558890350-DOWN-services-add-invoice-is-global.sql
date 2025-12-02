-- 1558890350 DOWN services-add-invoice-is-global
ALTER TABLE boarding_pass
DROP COLUMN invoice_is_global;

ALTER TABLE rental
DROP COLUMN invoice_is_global;

ALTER TABLE parcels
DROP COLUMN invoice_is_global;