-- 1589215390 DOWN update_app_client_1_6_1
DELETE FROM `systems_versions` WHERE (`Id` = '3');
UPDATE `systems_versions` SET `status` = '1' WHERE (`Id` = '2');