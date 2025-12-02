-- 1631746412 UP add-permit-parcel-report-in-POS

insert into sub_module (id, name, module_id, group_type, menu_type, status, created_by)
values (105, "app.parcel.report", 2,'parcel', "p_sub_parcel", 1 , 1);

insert into permission (id, name, description, sub_module_id, multiple, status, created_by)
values (223, "#report", "Ver reporte", 105, 0,1,1);