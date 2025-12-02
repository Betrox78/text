-- 1558984253 UP insurance permissions


INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(71, 'app.insurances', 1, 'operation', 'o_sub_catalogue', 1);



INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by) VALUES
(165,'#list', 'Ver listado',71 ,null,1),
(166,'#delete', 'Eliminar',71 ,165,1);