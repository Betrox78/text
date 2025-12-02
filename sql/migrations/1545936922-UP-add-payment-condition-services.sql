-- 1545936922 UP add-payment-condition-services
ALTER TABLE boarding_pass
ADD COLUMN payment_condition ENUM('credit', 'cash') NOT NULL DEFAULT 'cash';
ALTER TABLE rental
ADD COLUMN payment_condition ENUM('credit', 'cash') NOT NULL DEFAULT 'cash';
ALTER TABLE parcels
ADD COLUMN payment_condition ENUM('credit', 'cash') NOT NULL DEFAULT 'cash';
