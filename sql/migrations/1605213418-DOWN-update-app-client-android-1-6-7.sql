-- 1605213418 DOWN update-app-client-android-1-6-7
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.6.8' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.6.8' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.6.6' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.6.6' );