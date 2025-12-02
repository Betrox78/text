-- 1657845993 UP report-guiapp-available
insert into sub_module (id, name, module_id , group_type, menu_type, status, created_by)
values (122, 'app.reports.guiapp_available', 1, 'reports', 'r_sub_reportlogistics', 1, 1) ;

insert into permission (id, name , description, sub_module_id, status, created_by)
values (254, "#list", "Ver reporte de Guias PP disponibles", 122, 1, 1);