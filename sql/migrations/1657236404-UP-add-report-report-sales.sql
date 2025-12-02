-- 1657236404 UP add-report-report-sales
insert into sub_module (id, name, module_id , group_type, menu_type, status, created_by)
values (115, 'app.reports.parcel_sended', 1, 'reports', 'r_sub_reportlogistics', 1, 1) ;

insert into permission (id, name , description, sub_module_id, status, created_by)
values (246, "#list", "Ver reporte Paquetes enviados", 115, 1, 1);