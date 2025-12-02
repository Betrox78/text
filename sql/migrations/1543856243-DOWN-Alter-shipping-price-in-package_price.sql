-- 1543856243 DOWN Alter shipping price in package_price
ALTER TABLE `package_price` 
CHANGE COLUMN `shipping_type` `shipping_type` ENUM('parcel', 'courier') NOT NULL DEFAULT 'parcel' ;