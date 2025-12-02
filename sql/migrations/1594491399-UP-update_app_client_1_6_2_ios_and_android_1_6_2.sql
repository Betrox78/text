-- 1594491399 UP update_app_client_1_6_2_ios_and_android_1_6_2

UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'ANDROID' AND `version`!='1.6.4' );
UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'IOS' AND `version`!='1.6.4' );
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'ANDROID', '1.6.4', '1');
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'IOS', '1.6.4', '1');