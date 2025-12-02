-- 1550608698 UP add-column-contains-in-parcels-packages-table
ALTER TABLE parcels_packages
ADD COLUMN contains text DEFAULT NULL;