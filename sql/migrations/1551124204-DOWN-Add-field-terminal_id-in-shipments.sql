-- 1551124204 DOWN Add field terminal_id in shipments
ALTER TABLE `shipments` 
DROP FOREIGN KEY `fk_shipments_branchoffice_id`;
ALTER TABLE `shipments` 
DROP COLUMN `terminal_id`,
DROP INDEX `fk_shipments_branchoffice_id_idx` ;