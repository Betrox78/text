-- 1543885849 DOWN add-fields-payback-in-boarding_pass-rental-and-parcels
ALTER TABLE boarding_pass
DROP COLUMN payback;

ALTER TABLE parcels
DROP COLUMN payback;

ALTER TABLE rental
DROP COLUMN payback;