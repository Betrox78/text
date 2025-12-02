-- 1550275278 UP delete-parcels-allowed-table
ALTER TABLE parcels_packages
DROP FOREIGN KEY parcels_packages_parcel_allowed_id_fk;
ALTER TABLE parcels_packages
DROP COLUMN parcel_allowed_id;
DROP TABLE parcels_allowed;