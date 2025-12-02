-- 1552435056 DOWN Add fields yo driver_tracking
ALTER TABLE driver_tracking 
DROP FOREIGN KEY fk_driver_tracking_vehicle_id,
DROP FOREIGN KEY fk_driver_tracking_branchofficer_destiny_id,
DROP FOREIGN KEY fk_driver_tracking_branchoffice_origin_id;
ALTER TABLE driver_tracking 
DROP COLUMN action,
DROP COLUMN terminal_destiny_id,
DROP COLUMN terminal_origin_id,
DROP COLUMN rental_id,
DROP COLUMN vehicle_id,
DROP COLUMN type_tracking,
CHANGE COLUMN time_tracking time_tracking TEXT NULL DEFAULT NULL ,
CHANGE COLUMN status status TINYINT(4) NOT NULL DEFAULT 1 ,
DROP INDEX fk_driver_tracking_branchofficer_destiny_id_idx ,
DROP INDEX fk_driver_tracking_branchoffice_origin_id_idx ,
DROP INDEX fk_driver_tracking_vehicle_id_idx ;
