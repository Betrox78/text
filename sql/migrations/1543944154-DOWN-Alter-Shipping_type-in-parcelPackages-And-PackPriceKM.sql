-- 1543944154 DOWN Alter Shipping_type in parcelPackages And PackPriceKM
ALTER TABLE `parcels_packages` 
CHANGE COLUMN `shipping_type` `shipping_type` ENUM('parcel', 'courier') NOT NULL DEFAULT 'parcel' ;
ALTER TABLE `package_price_km` 
CHANGE COLUMN `shipping_type` `shipping_type` ENUM('parcel', 'courier') NOT NULL DEFAULT 'parcel' ;