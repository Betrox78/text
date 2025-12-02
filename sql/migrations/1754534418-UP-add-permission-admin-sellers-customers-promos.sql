-- 1754534418 UP add permission admin sellers customers promos
INSERT INTO sub_module(id, name, module_id, group_type, created_by) VALUES
(158, 'app.sellers_customers_promos', 1, 'commercial', 1);

INSERT INTO permission(name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
('#list', 'Ver lista', null, 158, 0, 1),
('#list.all', 'Ver todas las plazas', null, 158, 0, 1),
('#assign', 'Asignar', null, 158, 0, 1);