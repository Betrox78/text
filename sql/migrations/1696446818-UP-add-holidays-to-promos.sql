-- 1696446818 UP add-holidays-to-promos
ALTER TABLE `promos`
ADD COLUMN `is_holiday` TINYINT(1) NULL DEFAULT '0' AFTER `apply_ead`,
ADD COLUMN `holiday` ENUM('birthday') NULL DEFAULT NULL AFTER `is_holiday`,
ADD COLUMN `require_push_notifications` TINYINT(1) NULL DEFAULT '0' AFTER `holiday`;
