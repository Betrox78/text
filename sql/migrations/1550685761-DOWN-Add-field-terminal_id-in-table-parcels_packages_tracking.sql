-- 1550685761 DOWN Add field terminal_id in table parcels_packages_tracking
ALTER TABLE parcels_packages_tracking 
DROP FOREIGN KEY parcels_packages_tracking_branchoffice_id;
ALTER TABLE parcels_packages_tracking 
DROP COLUMN terminal_id,
DROP INDEX parcels_packages_tracking_branchoffice_id_idx ;