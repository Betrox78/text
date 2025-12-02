-- 1543336193 DOWN Add-county_id
ALTER TABLE customer_addresses
DROP COLUMN county_id;
ALTER TABLE customer_addresses
DROP COLUMN latitud;
ALTER TABLE customer_addresses
DROP COLUMN longitud;
ALTER TABLE `customer_addresses` 
DROP FOREIGN KEY `customer_addresses_county_id`;