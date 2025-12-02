-- 1727838501 UP module-permissions-inhouse-v2
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES (150, 'app.parcel.reception_inhouse_v2', 2, 'parcel', 'p_sub_parcel', 1);
INSERT INTO permission(name, description, sub_module_id, multiple, created_by)
VALUES ('#create', 'Documentar paqueteria in house v2', 150, 0, 1);