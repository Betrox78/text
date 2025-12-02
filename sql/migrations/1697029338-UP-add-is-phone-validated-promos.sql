-- 1697029338 UP add-is-phone-validated-promos
ALTER TABLE `promos`
ADD COLUMN `apply_only_phone_validated` TINYINT(1) NULL DEFAULT '0';
