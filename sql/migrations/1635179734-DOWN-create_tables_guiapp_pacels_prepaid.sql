-- 1635179734 DOWN create_tables_guiapp_pacels_prepaid

ALTER TABLE `payment`
DROP FOREIGN KEY `fk_parcels_prepaid_id`;
ALTER TABLE `payment`
DROP COLUMN `parcels_prepaid_id`,
DROP INDEX `fk_parcels_prepaid_id_idx` ;

ALTER TABLE `tickets`
DROP FOREIGN KEY `fk_parcel_prepaid_id`;
ALTER TABLE `tickets`
DROP COLUMN `parcel_prepaid_id`,
DROP INDEX `fk_parcel_prepaid_id_idx` ;


DROP TABLE parcels_prepaid;
DROP TABLE parcels_prepaid_detail;


