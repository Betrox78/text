-- 1543370641 DOWN delete-excess-price-id-on-parcels-package
ALTER TABLE parcels_packages
ADD CONSTRAINT fk_excess_price_id FOREIGN KEY (excess_price_id) REFERENCES package_price(id);
