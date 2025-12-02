-- 1558890350 UP services-add-invoice-is-global
ALTER TABLE boarding_pass
ADD COLUMN invoice_is_global BIT(1) NOT NULL DEFAULT 0;

ALTER TABLE rental
ADD COLUMN invoice_is_global BIT(1) NOT NULL DEFAULT 0;

ALTER TABLE parcels
ADD COLUMN invoice_is_global BIT(1) NOT NULL DEFAULT 0;