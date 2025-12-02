-- 1555525293 UP add-process-operative-permissions
INSERT INTO sub_module (id, name, module_id, group_type, menu_type, created_by) VALUES
(60, 'app.shipment-toload', 3, 'travels_log', null, 1),
(61, 'app.shipment-todownload', 3, 'travels_log', null, 1);

INSERT INTO permission (id,name, description, dependency_id, sub_module_id, created_by) VALUES
(145,'#list','Ver lista', null,  60, 1),
(146,'.create','Iniciar embarque', 145,  60, 1),
(147,'#list','Ver lista', null, 61, 1),
(148,'.create','Iniciar desembarque', 147, 61, 1);