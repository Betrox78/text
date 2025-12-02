-- 1594491399 DOWN update_app_client_1_6_2_ios_and_android_1_6_2
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.6.4' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.6.4' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.6.1' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.6' );