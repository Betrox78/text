-- 1560992634 DOWN add promo id column in rental parcel boardingpass tables
ALTER TABLE boarding_pass
DROP FOREIGN KEY boarding_pass_promo_id_fk,
DROP COLUMN promo_id;

ALTER TABLE boarding_pass
ADD COLUMN discount_code_id int(11) DEFAULT NULL AFTER total_amount;

ALTER TABLE parcels
DROP FOREIGN KEY parcels_promo_id_fk,
DROP COLUMN promo_id;

ALTER TABLE parcels
ADD COLUMN discount_code_id int(11) DEFAULT NULL AFTER total_amount;

ALTER TABLE rental
DROP FOREIGN KEY rental_promo_id_fk,
DROP COLUMN promo_id;

ALTER TABLE rental
ADD COLUMN discount_code decimal(12,2) DEFAULT 0.00 AFTER total_amount;