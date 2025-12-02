-- 1609781652 UP add_new_version_1_7_0_app_client

UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'ANDROID' AND `version`!='1.7.0' );
UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'IOS' AND `version`!='1.7.0' );
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'ANDROID', '1.7.0', '1');
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'IOS', '1.7.0', '1');