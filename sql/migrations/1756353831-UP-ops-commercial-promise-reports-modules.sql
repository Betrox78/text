-- 1756353831 UP ops commercial promise reports modules
INSERT INTO sub_module (id, name, module_id, group_type, created_by) VALUES
(159, 'app.reports.commercial_promise', 3, 'last_mile', 1),
(160, 'app.reports.commercial_promise.global', 3, 'last_mile', 1);

INSERT INTO permission (name, description, sub_module_id, created_by) VALUES
('#list', 'Ver reporte', 159, 1),
('#list', 'Ver reporte', 160, 1);