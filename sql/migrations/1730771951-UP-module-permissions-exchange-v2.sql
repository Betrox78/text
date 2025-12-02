-- 1730771951 UP module permissions exchange v2
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES (151, 'app.parcel_guiapp.exchange_v2', 2, 'guia_pp', 'g_sub_guia_pp', 1);
INSERT INTO permission(name, description, sub_module_id, multiple, created_by)
VALUES ('#exchange', 'Canje de guias prepagadas v2', 151, 0, 1);