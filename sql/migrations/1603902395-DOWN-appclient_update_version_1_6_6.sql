-- 1603902395 DOWN appclient_update_version_1_6_6
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.6.6' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.6.6' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.6.5' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.6.5' );