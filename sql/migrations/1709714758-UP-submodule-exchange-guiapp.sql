-- 1709714758 UP submodule exchange guiapp
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES (148, 'app.parcel_guiapp.exchange', 2, 'guia_pp', 'g_sub_guia_pp', 1);

INSERT INTO permission(name, description, sub_module_id, created_by)
VALUES ('#exchange', 'Canje de guias prepagadas', 148, 1);