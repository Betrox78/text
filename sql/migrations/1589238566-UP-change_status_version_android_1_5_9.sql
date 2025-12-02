-- 1589238566 UP change_status_version_android_1_5_9
UPDATE `systems_versions` SET `status` = '0' WHERE `version` = '1.5.9' AND `os` = 'ANDROID' ;
INSERT INTO `systems_versions` (`name`, `os`, `version`,`status`) VALUES ('AbordoMovil', 'IOS', '1.6', '1');
