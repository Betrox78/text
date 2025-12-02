-- 1654796076 UP permit-report-guiapp

insert into sub_module (id, name, module_id , group_type, menu_type, status, created_by)
values (113, 'app.reports.guiapp', 1, 'reports', 'r_sub_reportsales', 1, 1) ;

insert into permission (id, name , description, sub_module_id, status, created_by)
values (241, "#list", "Ver reporte guia PP", 113, 1, 1);
