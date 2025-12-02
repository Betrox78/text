-- 1607799772 DOWN appclient_update_v_1_6_9
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.6.9' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.6.9' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.6.8' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.6.8' );