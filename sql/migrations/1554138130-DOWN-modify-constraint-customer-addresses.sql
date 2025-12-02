-- 1554138130 DOWN modify-constraint-customer-addresses

ALTER TABLE `customer_addresses`
DROP FOREIGN KEY `customer_addresses_county_id`;

ALTER TABLE `customer_addresses`
ADD CONSTRAINT `customer_addresses_county_id`
  FOREIGN KEY (`county_id`)
  REFERENCES `county` (`id`);

ALTER TABLE `parcels_packages`
DROP FOREIGN KEY `parcels_packages_pets_sizes_id_fk`;

ALTER TABLE `parcels_packages`
ADD CONSTRAINT `parcels_packages_pets_sizes_id_fk`
  FOREIGN KEY (`pets_sizes_id`)
  REFERENCES `pets_sizes` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;