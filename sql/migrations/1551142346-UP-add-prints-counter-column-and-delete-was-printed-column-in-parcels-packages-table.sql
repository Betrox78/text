-- 1551142346 UP add-prints-counter-column-and-delete-was-printed-column-in-parcels-packages-table
ALTER TABLE parcels_packages
ADD COLUMN prints_counter int(11) NOT NULL DEFAULT 0 AFTER package_code,
DROP COLUMN was_printed;