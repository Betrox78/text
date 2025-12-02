-- 1686267063 UP add permision and module parcel reception in house
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES (138, 'app.parcel.reception_inhouse', 2, 'parcel', 'p_sub_parcel', 1);

INSERT INTO permission(name, description, sub_module_id, multiple, created_by)
VALUES ('#create', 'Documentar paqueteria in house', 138, 0, 1);
