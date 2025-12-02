-- 1717875368 DOWN add-is-old-col-pkgs
ALTER TABLE parcels_packages
DROP COLUMN is_old;

ALTER TABLE parcels_prepaid_detail
DROP COLUMN is_old;
