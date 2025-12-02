-- 1555113168 UP operative permissions
ALTER TABLE sub_module
MODIFY COLUMN group_type enum('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config', 'buses', 'travels_log');

INSERT INTO sub_module (id, name, module_id, group_type, menu_type, created_by) VALUES
(56, 'app.reports.inventory', 3, 'reports', 'r_sub_reports', 1),
(57, 'app.daily-logs', 3, 'travels_log', null, 1),
(58, 'app.shipment-load', 3, 'travels_log', null, 1),
(59, 'app.shipment-download', 3, 'travels_log', null, 1);

INSERT INTO permission (id,name, description, dependency_id, sub_module_id, created_by) VALUES
(141,'#list','Ver reporte', null,  56, 1),
(142,'#list','Ver lista', null,  57, 1),
(143,'#list','Ver lista', null, 58, 1),
(144,'#list','Ver lista', null, 59, 1);