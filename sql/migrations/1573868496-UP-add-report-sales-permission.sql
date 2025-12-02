-- 1573868496 UP add-report-sales-permission
INSERT INTO sub_module (id, name, module_id, group_type, created_by) VALUES
(95, 'app.reports.sales', 2, 'reports', 1);

INSERT INTO permission (id, name, description, sub_module_id, created_by) VALUES
(197, '#list', 'Ver reporte', 95, 1);
