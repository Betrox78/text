-- 1563563243 UP cubic-permissions-generic

INSERT INTO sub_module (id, name, module_id, group_type, menu_type, created_by) VALUES
(74, 'app.cuber', 1, 'operation', 'o_sub_catalogue', 1);

INSERT INTO permission (id,name, description, dependency_id, sub_module_id, created_by) VALUES
(173, '#list', 'Ver listado', NULL, 74, 0),
(174, '#delete', 'Eliminar', 173, 74, 0),
(175, '#add', 'Registrar', 173, 74, 0);