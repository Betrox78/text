-- 1573172690 UP add-reports-permission
INSERT INTO sub_module (id, name, module_id, group_type, created_by) VALUES
(77, 'app.reports.ingresosparcel', 1, 'reports', 1),
(78, 'app.reports.month', 1, 'reports', 1),
(79, 'app.reports.general', 1, 'reports', 1);

INSERT INTO permission (id, name, description, sub_module_id, created_by) VALUES
(179, '#list', 'Reporte de paquetería por fecha de ingresos', 77, 1),
(180, '#list', 'Reporte mensual servicios', 78, 1),
(181, '#list', 'Reporte general acumulado', 79, 1);