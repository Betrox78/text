-- 1597965739 DOWN update_version_app_client_1_6_5
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.6.5' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.6.5' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.6.4' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.6.4' );