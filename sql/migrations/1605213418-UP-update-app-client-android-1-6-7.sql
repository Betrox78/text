-- 1605213418 UP update-app-client-android-1-6-7
UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'ANDROID' AND `version`!='1.6.8' );
UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'IOS' AND `version`!='1.6.8' );
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'ANDROID', '1.6.8', '1');
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'IOS', '1.6.8', '1');