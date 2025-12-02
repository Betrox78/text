-- 1597965739 UP update_version_app_client_1_6_5

UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'ANDROID' AND `version`!='1.6.5' );
UPDATE `systems_versions` SET `status` = '0' WHERE (`os` = 'IOS' AND `version`!='1.6.5' );
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'ANDROID', '1.6.5', '1');
INSERT INTO `systems_versions` (`name`, `os`, `version`, `status`) VALUES ('AbordoMovil', 'IOS', '1.6.5', '1');