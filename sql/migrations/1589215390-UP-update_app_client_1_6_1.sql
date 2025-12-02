-- 1589215390 UP update_app_client_1_6_1

UPDATE `systems_versions` SET `status` = '0' WHERE (`version` = '1.6');
INSERT INTO systems_versions (name, os, version, status) VALUES ('AbordoMovil', 'ANDROID', '1.6.1', '1');