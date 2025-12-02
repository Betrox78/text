-- 1592846996 DOWN alter_parcels_manifest_drive_vehicle
ALTER TABLE `parcels_manifest`
DROP COLUMN `drive_name`,
DROP COLUMN `vehicle_serial_num`,
DROP COLUMN `branchoffice_origin`;

ALTER TABLE  `parcels_rad_ead`
DROP COLUMN `confirme_rad` TINYINT(4) NULL DEFAULT 0 AFTER `updated_by`;

