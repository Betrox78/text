-- 1560992634 UP add promo id column in rental parcel boardingpass tables
ALTER TABLE boarding_pass
DROP COLUMN discount_code_id;

ALTER TABLE boarding_pass
ADD COLUMN promo_id int(11) DEFAULT NULL AFTER total_amount,
ADD CONSTRAINT boarding_pass_promo_id_fk FOREIGN KEY(promo_id) REFERENCES promos(id);

ALTER TABLE parcels
DROP COLUMN discount_code_id;

ALTER TABLE parcels
ADD COLUMN promo_id int(11) DEFAULT NULL AFTER total_amount,
ADD CONSTRAINT parcels_promo_id_fk FOREIGN KEY(promo_id) REFERENCES promos(id);

ALTER TABLE rental
DROP COLUMN discount_code;

ALTER TABLE rental
ADD COLUMN promo_id int(11) DEFAULT NULL AFTER total_amount,
ADD CONSTRAINT rental_promo_id_fk FOREIGN KEY(promo_id) REFERENCES promos(id);