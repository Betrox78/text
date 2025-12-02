-- 1706140810 UP submodule parcel reception v2
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(146, 'app.app.parcel.reception_v2', 2, 'parcel', 'p_sub_parcel', 1);

INSERT into permission (name, description, sub_module_id,created_by)
VALUES ('#create', 'Documentar paqueteria v2', 146, 1);