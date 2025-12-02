-- 1711988187 UP add-travel-logs-cost-rpt-perm
insert into sub_module (name , module_id , group_type , menu_type , status , created_by)
values ('app.reports.travel_logs_cost' , 1 , 'reports', 'r_sub_reportlogistics' , 1 , 1 );

insert into permission (name , description , sub_module_id , multiple , status , created_by)
values ('#list' , 'Ver reporte' , (select id from sub_module where name = 'app.reports.travel_logs_cost'), 0 , 1 , 1 );