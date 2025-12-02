-- 1553967632 UP add-config-rental-permission

INSERT INTO sub_module (id, name, module_id, group_type, menu_type, created_by) VALUES
(52, 'app.generalconfig.rental', 1, 'config', 'c_sub_generalconfig', 1);

INSERT INTO permission (id,name, description, dependency_id, sub_module_id, created_by) VALUES
(137,'#list','Modificar', null,  52, 1);