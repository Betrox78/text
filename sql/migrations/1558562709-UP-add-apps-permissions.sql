-- 1558562709 UP add apps permissions
INSERT INTO module(name, description, created_by) VALUES
('app_operation', 'Acceso a la aplicación operativa (maletero)', 1),
('app_driver', 'Acceso a la aplicación del chofer', 1);

INSERT INTO sub_module(name, module_id, group_type, created_by) VALUES
('all', 4, 'admin', 1),
('all', 5, 'admin', 1);

INSERT INTO permission(name, description, sub_module_id, created_by) VALUES
('#all', 'Acceso a la aplicación', 62, 1),
('#all', 'Acceso a la aplicación', 63, 1);