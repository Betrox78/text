-- 1552431591 DOWN add-invoice
ALTER TABLE boarding_pass
DROP FOREIGN KEY boarding_pass_invoice_id_fk,
DROP COLUMN invoice_id;

ALTER TABLE rental
DROP FOREIGN KEY rental_invoice_id_fk,
DROP COLUMN invoice_id;

ALTER TABLE parcels
DROP FOREIGN KEY parcels_invoice_id_fk,
DROP COLUMN invoice_id;

DROP TABLE invoice;