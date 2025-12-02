-- 1554580507 UP add-branchoffice-parcels
ALTER TABLE parcels
ADD COLUMN branchoffice_id INT(11) DEFAULT NULL AFTER customer_id;
ALTER TABLE parcels
ADD INDEX `parcels_branchoffice_id_fk_idx` (`branchoffice_id` ASC);
ALTER TABLE `parcels`
ADD CONSTRAINT `parcels_branchoffice_id_fk_idx`
  FOREIGN KEY (`branchoffice_id`)
  REFERENCES `branchoffice` (`id`)
  ON DELETE CASCADE;


UPDATE parcels SET branchoffice_id = terminal_origin_id;