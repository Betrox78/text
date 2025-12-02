-- 1750921548 UP add permission ops parcels manifest
INSERT INTO sub_module(id, name, module_id, group_type, created_by) VALUES
(157, 'app.parcels-manifest', 3, 'reports', 1);

INSERT INTO permission(name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
('#list', 'Ver lista', null, 157, 0, 1);