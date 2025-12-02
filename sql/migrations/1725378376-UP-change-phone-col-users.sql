-- 1725378376 UP change-phone-col-users
ALTER TABLE `users`
CHANGE COLUMN `phone` `phone` VARCHAR(13) NOT NULL DEFAULT '';