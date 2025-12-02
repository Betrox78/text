-- 1554138130 UP modify-constraint-customer-addresses

ALTER TABLE `customer_addresses`
DROP FOREIGN KEY `customer_addresses_county_id`;

ALTER TABLE `customer_addresses`
ADD CONSTRAINT `customer_addresses_county_id`
  FOREIGN KEY (`county_id`)
  REFERENCES `county` (`id`) ON DELETE CASCADE;

ALTER TABLE `parcels_packages`
DROP FOREIGN KEY `parcels_packages_pets_sizes_id_fk`;

ALTER TABLE `parcels_packages`
ADD CONSTRAINT `parcels_packages_pets_sizes_id_fk`
  FOREIGN KEY (`pets_sizes_id`)
  REFERENCES `pets_sizes` (`id`)
  ON DELETE CASCADE;