-- 1678155203 UP insert-submodule-and-permit
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(135, 'app.prepaid_packages', 1, 'operation', 'o_sub_catalogue', 1);
INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by)
VALUES (271,'#list', 'Consultar',135 ,null,1);