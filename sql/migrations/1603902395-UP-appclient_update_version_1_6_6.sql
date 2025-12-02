-- 1603902395 UP appclient_update_version_1_6_6
UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'ANDROID' AND `version`!='1.6.6' );
UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'IOS' AND `version`!='1.6.6' );
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'ANDROID', '1.6.6', '1');
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'IOS', '1.6.6', '1');