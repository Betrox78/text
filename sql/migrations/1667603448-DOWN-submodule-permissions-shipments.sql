-- 1667603448 DOWN submodule-permissions-shipments
DELETE FROM permission WHERE id = 261;
DELETE FROM permission WHERE id = 262;
DELETE FROM permission WHERE id = 263;
DELETE FROM permission WHERE id = 264;
DELETE FROM permission WHERE id = 265;
DELETE FROM permission WHERE id = 266;

DELETE FROM sub_module WHERE id = 129;
DELETE FROM sub_module WHERE id = 130;
DELETE FROM sub_module WHERE id = 131;
DELETE FROM sub_module WHERE id = 132;

 INSERT INTO sub_module VALUES
(58, 'app.shipment-load', 3, 'travels_log', NULL, 1, '2019-05-27 14:43:47', 1, NULL, NULL),
(59, 'app.shipment-download', 3, 'travels_log', NULL, 1, '2019-05-27 14:43:47', 1, NULL, NULL),
(60, 'app.shipment-toload', 3, 'travels_log', NULL, 1, '2019-05-27 14:43:48', 1, NULL, NULL),
(61, 'app.shipment-todownload', 3, 'travels_log', NULL, 1, '2019-05-27 14:43:48', 1, NULL, NULL);

INSERT INTO permission VALUES
(143, '#list', 'Ver lista', NULL, 58, 0, 1, '2019-05-27 14:43:48', 1, NULL, NULL),
(144, '#list', 'Ver lista', NULL, 59, 0, 1, '2019-05-27 14:43:48', 1, NULL, NULL),
(145, '#list', 'Ver lista', NULL, 60, 0, 1, '2019-05-27 14:43:48', 1, NULL, NULL),
(146, '.create', 'Iniciar embarque', 145, 60, 0, 1, '2019-05-27 14:43:48', 1, NULL, NULL),
(147, '#list', 'Ver lista', NULL, 61, 0, 1, '2019-05-27 14:43:48', 1, NULL, NULL),
(148, '.create', 'Iniciar desembarque', 147, 61, 0, 1, '2019-05-27 14:43:48', 1, NULL, NULL);