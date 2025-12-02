-- 1754601602 UP add permission admin sellers customers promos assign
UPDATE permission SET name = '#assign_plaza', description = 'Asignar plaza' WHERE sub_module_id = 158 AND name = '#assign';

INSERT INTO permission(name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
('#assign_seller', 'Asignar vendedor', null, 158, 0, 1);