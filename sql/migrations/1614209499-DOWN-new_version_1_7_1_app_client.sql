-- 1614209499 DOWN new_version_1_7_1_app_client
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.7.1' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.7.1' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.7.0' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.7.0' );