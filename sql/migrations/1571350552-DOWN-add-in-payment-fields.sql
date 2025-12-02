-- 1571350552 DOWN add-in-payment-fields
ALTER TABLE boarding_pass
    DROP COLUMN in_payment;

ALTER TABLE parcels
    DROP COLUMN in_payment;

ALTER TABLE rental
    DROP COLUMN in_payment;