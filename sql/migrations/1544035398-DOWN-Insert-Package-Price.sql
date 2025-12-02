-- 1544035398 DOWN Insert-Package-Price
update package_price set status=3 where shipping_type='frozen';
ALTER TABLE `package_price` 
CHANGE COLUMN `created_by` `created_ by` INT(11) NULL DEFAULT NULL ;
