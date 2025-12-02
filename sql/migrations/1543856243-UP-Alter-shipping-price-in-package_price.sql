-- 1543856243 UP Alter shipping price in package_price
ALTER TABLE `package_price` 
CHANGE COLUMN `shipping_type` `shipping_type` ENUM('parcel', 'courier', 'pets', 'frozen') NOT NULL DEFAULT 'parcel' ;