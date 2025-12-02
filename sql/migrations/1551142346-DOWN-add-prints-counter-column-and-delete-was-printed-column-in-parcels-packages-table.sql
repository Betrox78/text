-- 1551142346 DOWN add-prints-counter-column-and-delete-was-printed-column-in-parcels-packages-table
ALTER TABLE parcels_packages
ADD COLUMN was_printed tinyint(1) NOT NULL DEFAULT 0 AFTER package_code,
DROP COLUMN prints_counter;