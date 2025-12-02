-- 1710916430 DOWN add promo id parcels packages table
ALTER TABLE parcels_packages
DROP FOREIGN KEY fk_parcels_packages_promo_id,
DROP COLUMN promo_id;