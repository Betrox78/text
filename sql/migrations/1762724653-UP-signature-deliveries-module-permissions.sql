-- 1762724653 UP signature deliveries module permissions
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(163, 'app.reports.signature_deliveries', 1, 'reports', 'r_sub_reportlogistics', 1);

INSERT INTO permission(name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
('#report', 'Ver reporte', null, 163, 0, 1),
('#all_terminals', 'Todas las terminales', null, 163, 0, 1);