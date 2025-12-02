-- 1680050453 UP inserts
-- first we inster a new enum
alter table sub_module
MODIFY COLUMN group_type enum('general','admin',
    'operation','logistic','vans','parcel','risks',
    'reports','config','buses','travels_log','billing', 'guia_pp', 'prepaid');
insert into sub_module (id, name , module_id , group_type ,created_by)
    values (136, 'app.prepaid', 2 , 'prepaid',1);
insert into permission (id, name , description, sub_module_id , created_by)
    values (272, '#create' , 'Venta de paquetes prepago', 136,1);
-- ////////
insert into sub_module (id, name, module_id, group_type, status, created_by)
    values (137, 'app.reservation_prepaid', 2, 'prepaid', 1, 1 );
insert into permission (id, name , description, sub_module_id, status, created_by)
    values(273, '#detail', 'Ver detalle de reservación prepago', 137, 1 , 1);
insert into permission (id, name , description, dependency_id, sub_module_id, status, created_by)
    values(274, '#print', 'Re-impresiones',  273,137, 1, 1);
insert into permission (id, name , description, dependency_id, sub_module_id, status, created_by)
    values(275, '#cancel', 'Cancelación',  273,137, 1, 1);