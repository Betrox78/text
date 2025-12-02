-- 1548208398 DOWN migrate Add Field Boarding_pas_id in parcels
ALTER TABLE `parcels` 
DROP FOREIGN KEY `parcels_boarding_pass_id_fk`;
ALTER TABLE `parcels` 
DROP COLUMN `boarding_pass_id`,
DROP INDEX `parcels_boarding_pass_id_fk_idx` ;