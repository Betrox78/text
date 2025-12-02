-- 1717875368 UP add-is-old-col-pkgs
ALTER TABLE `parcels_packages`
ADD COLUMN `is_old` TINYINT(1) NULL DEFAULT '0';

ALTER TABLE `parcels_prepaid_detail`
ADD COLUMN `is_old` TINYINT(1) NULL DEFAULT '0';
