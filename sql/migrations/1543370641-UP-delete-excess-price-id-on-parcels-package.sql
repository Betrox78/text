-- 1543370641 UP delete-excess-price-id-on-parcels-package
ALTER TABLE parcels_packages
DROP FOREIGN KEY fk_excess_price_id;