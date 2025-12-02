-- 1614448093 DOWN update_1_7_2_app_client
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.7.2' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.7.2' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.7.1' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.7.1' );