-- 1545936922 DOWN add-payment-condition-services
ALTER TABLE boarding_pass
DROP COLUMN payment_condition;
ALTER TABLE rental
DROP COLUMN payment_condition;
ALTER TABLE parcels
DROP COLUMN payment_condition;