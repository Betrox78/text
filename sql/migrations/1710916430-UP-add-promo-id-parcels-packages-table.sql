-- 1710916430 UP add promo id parcels packages table
ALTER TABLE parcels_packages
ADD COLUMN promo_id INT(11) DEFAULT NULL AFTER status,
ADD CONSTRAINT fk_parcels_packages_promo_id FOREIGN KEY (promo_id) REFERENCES promos(id) ON DELETE NO ACTION ON UPDATE NO ACTION;