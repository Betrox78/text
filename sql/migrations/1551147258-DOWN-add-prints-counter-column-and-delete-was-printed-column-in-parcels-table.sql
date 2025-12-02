-- 1551147258 DOWN add-prints-counter-column-and-delete-was-printed-column-in-parcels-table
ALTER TABLE parcels
ADD COLUMN was_printed tinyint(1) NOT NULL DEFAULT 0 AFTER parcel_tracking_code,
DROP COLUMN prints_counter;