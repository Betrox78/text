-- 1551147258 UP add-prints-counter-column-and-delete-was-printed-column-in-parcels-table
ALTER TABLE parcels
ADD COLUMN prints_counter int(11) NOT NULL DEFAULT 0 AFTER parcel_tracking_code,
DROP COLUMN was_printed;