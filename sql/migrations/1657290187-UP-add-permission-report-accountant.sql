-- 1657290187 UP add-permission-report-accountant
insert into sub_module (id, name, module_id , group_type, menu_type, status, created_by)
values (118, 'app.reports.travelsAccountant', 1, 'reports', 'r_sub_reportlogistics', 1, 1) ;

insert into permission (id, name , description, sub_module_id, status, created_by)
values (247, "#list", "Ver reporte de venta de boletos", 118, 1, 1);