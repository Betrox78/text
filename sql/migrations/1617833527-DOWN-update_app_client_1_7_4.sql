-- 1617833527 DOWN update_app_client_1_7_4
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.7.4' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.7.4' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.7.3' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.7.3' );