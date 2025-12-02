-- 1553374130 UP cancel-reason-permissions
INSERT INTO sub_module
(id, name, module_id, group_type, menu_type, created_by) VALUES
(49,'app.cancel-reason', 1, 'parcel', 'p_sub_parcel', 1);

INSERT INTO permission
(id,name, description, dependency_id, sub_module_id, created_by) VALUES
(131,'#list','Ver listado', null,  49, 1),
(132,'#add','Registrar', 131, 49, 1),
(133,'#edit','Modificar', 131, 49, 1),
(134,'#delete','Eliminar', 131, 49, 1);