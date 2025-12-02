-- 1609781652 DOWN add_new_version_1_7_0_app_client

DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.7.0' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.7.0' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.6.9' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.6.9' );