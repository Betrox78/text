-- 1550685761 UP Add field terminal_id in table parcels_packages_tracking
ALTER TABLE parcels_packages_tracking 
ADD COLUMN terminal_id INT(11) NULL AFTER parcel_package_id,
ADD INDEX parcels_packages_tracking_branchoffice_id_idx (terminal_id ASC);

ALTER TABLE parcels_packages_tracking 
ADD CONSTRAINT parcels_packages_tracking_branchoffice_id
  FOREIGN KEY (terminal_id)
  REFERENCES branchoffice (id)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;