-- 1716343708 DOWN breakdown columns packages and packings
ALTER TABLE parcels_packages
DROP FOREIGN KEY fk_parcels_packages_excess_promo_id,
DROP COLUMN excess_promo_id,
DROP COLUMN iva,
DROP COLUMN excess_discount;

ALTER TABLE parcels_packings
DROP COLUMN iva;