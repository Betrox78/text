-- 1543095661 DOWN add excess fields in parcels_packages and foreign key to excess_price_id
ALTER TABLE parcels_packages
DROP FOREIGN KEY fk_excess_price_id,
DROP COLUMN excess_price_id,
DROP COLUMN excess_price,
DROP COLUMN excess_cost;