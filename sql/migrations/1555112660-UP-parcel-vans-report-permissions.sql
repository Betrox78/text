-- 1555112660 UP parcel vans report permissions

INSERT INTO sub_module (id, name, module_id, group_type, menu_type, created_by) VALUES
(53, 'app.reports.travels', 1, 'reports', 'r_sub_reports', 1),
(54, 'app.reports.parcel', 1, 'reports', 'r_sub_reports', 1),
(55, 'app.reports.vans', 1, 'reports', 'r_sub_reports', 1);

INSERT INTO permission (id,name, description, dependency_id, sub_module_id, created_by) VALUES
(138,'#list','Ver reporte', null,  53, 1),
(139,'#list','Ver reporte', null, 54, 1),
(140,'#list','Ver reporte', null,  55, 1);