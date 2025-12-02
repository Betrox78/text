-- 1609949363 UP permiso-reporte-prepago
insert into sub_module (id, name , module_id, group_type , menu_type , created_by ) values (102 , 'app.reports.prepaid_report', 1 , 'reports','r_sub_reportothers' , 1);

insert into permission (name , description , sub_module_id , multiple , status , created_by) values ('#list' , 'Consultar' , 102 , 0 ,1 , 1);