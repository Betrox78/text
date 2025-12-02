-- 1614275035 UP report-travel-seller-permissions

insert into sub_module (name , module_id , group_type , menu_type , status , created_by)
values ('app.reports.travels_sellers' , 1 , 'reports', 'r_sub_reportsales' , 1 , 1 );

insert into permission (name , description , sub_module_id , multiple , status , created_by)
values ('#list' , 'Ver reporte' , (select id from sub_module where name = 'app.reports.travels_sellers'), 0 , 1 , 1 );