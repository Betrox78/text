-- 1688191800 UP
insert into sub_module (id, name, module_id, group_type, menu_type, created_by)
values (139, 'app.reports.prepaid_sales', 1, 'prepaid', 'r_sub_reportsales', 1);

insert into permission (name, description, sub_module_id, status, created_by)
values ('#list', 'Ver reporte', 139, 1, 1);