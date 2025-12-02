-- 1696446818 DOWN add-holidays-to-promos
ALTER TABLE `promos`
DROP COLUMN `require_push_notifications`,
DROP COLUMN `holiday`,
DROP COLUMN `is_holiday`;

