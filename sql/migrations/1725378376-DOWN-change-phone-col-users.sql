-- 1725378376 DOWN change-phone-col-users
ALTER TABLE `users`
CHANGE COLUMN `phone` `phone` CHAR(10) NOT NULL DEFAULT '';