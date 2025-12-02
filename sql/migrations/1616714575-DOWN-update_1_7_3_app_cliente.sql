-- 1616714575 DOWN update_1_7_3_app_cliente
DELETE FROM `systems_versions` WHERE (`os` = 'ANDROID' AND `version` = '1.7.3' );
DELETE FROM `systems_versions` WHERE (`os` = 'IOS' AND `version` = '1.7.3' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'ANDROID' AND `version`='1.7.2' );
UPDATE `systems_versions` SET `status` = '1' WHERE (`os` = 'IOS' AND `version`='1.7.2' );