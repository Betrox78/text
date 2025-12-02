-- 1667603448 UP submodule-permissions-shipments
DELETE FROM sub_module WHERE id = 58;
DELETE FROM sub_module WHERE id = 59;
DELETE FROM sub_module WHERE id = 60;
DELETE FROM sub_module WHERE id = 61;

DELETE FROM permission WHERE id = 143;
DELETE FROM permission WHERE id = 143;
DELETE FROM permission WHERE id = 144;
DELETE FROM permission WHERE id = 145;
DELETE FROM permission WHERE id = 146;
DELETE FROM permission WHERE id = 147;
DELETE FROM permission WHERE id = 148;

INSERT INTO sub_module(id, name, module_id, group_type, created_by) VALUES
(129, 'app.shipment_pending_to_load', 3, 'travels_log', 1),
(130, 'app.shipment_pending_to_download', 3, 'travels_log', 1),
(131, 'app.shipment_load_history', 3, 'travels_log', 1),
(132, 'app.shipment_download_history', 3, 'travels_log', 1);

INSERT INTO permission(id, name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
(261, '#list', 'Ver lista', null, 129, 0, 1),
(262, '#list', 'Ver lista', null, 130, 0, 1),
(263, 'shipment_load', 'Iniciar embarque', 255, 129, 0, 1),
(264, 'shipment_download', 'Iniciar desembarque', 256, 130, 0, 1),
(265, '#list', 'Ver lista', null, 131, 0, 1),
(266, '#list', 'Ver lista', null, 132, 0, 1);