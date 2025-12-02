-- 1592846996 UP alter_parcels_manifest_drive_vehicle
ALTER TABLE `parcels_manifest`
ADD COLUMN `vehicle_serial_num` VARCHAR(100) NOT NULL AFTER `updated_by`,
ADD COLUMN `drive_name` VARCHAR(100) NOT NULL AFTER `updated_by`,
ADD COLUMN `branchoffice_origin` VARCHAR(45) NULL AFTER `updated_by`;


ALTER TABLE `parcels_rad_ead`
ADD COLUMN `confirme_rad` TINYINT(4) NULL DEFAULT 0 AFTER `updated_by`;

