-- 1543336193 UP Add-county_id
ALTER TABLE `customer_addresses` 
ADD COLUMN `county_id` int(11) DEFAULT NULL AFTER `city_id`;
ALTER TABLE `customer_addresses`
ADD  COLUMN `latitud` VARCHAR(20) DEFAULT NULL AFTER `updated_by` ,
ADD  COLUMN `longitud` VARCHAR(20) DEFAULT NULL AFTER `updated_by`;

ALTER TABLE `customer_addresses` 
ADD CONSTRAINT `customer_addresses_county_id`
  FOREIGN KEY (`county_id`)
  REFERENCES `county` (`id`);
