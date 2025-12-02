-- 1558838293 UP permissions phase 2

INSERT into permission (id, name, description, dependency_id,sub_module_id,created_by) VALUES
(151, '.view', 'Ver detalle',54,15,1);


INSERT INTO sub_module (id, name, module_id, group_type,created_by)
VALUES(64, 'app.alliance',1,'admin',1);


INSERT into permission (id, name, description, dependency_id,sub_module_id,created_by) VALUES
(152, '#list', 'Ver lista',null,64,1);
INSERT into permission (id, name, description, dependency_id,sub_module_id,created_by) VALUES
(153, '.add', 'Registrar',150,64,1),
(154, '.edit', 'Modificar',150,64,1),
(155, '#delete', 'Registrar',150,64,1);


INSERT INTO sub_module (id, name, module_id, group_type,created_by)
VALUES(65, 'app.typecategories',1,'admin',1);

INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by) VALUES
(156, '#list', 'Ver listado',65 ,null,1),
(157, '#add', 'Registrar', 65,154,1),
(158, '#edit', 'Modificar', 65,154,1),
(159, '#delete', 'Eliminar', 65,154,1);


INSERT INTO sub_module (id, name, module_id, group_type,created_by)
VALUES(66, 'app.attendance',1,'reports',1);

INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by) VALUES
(160, '#list', 'Ver listado',66 ,null,1);

INSERT INTO sub_module (id, name, module_id, group_type,created_by)
VALUES(67, 'app.driverattendance',1,'reports',1);

INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by) VALUES
(161, '#list', 'Ver listado',67 ,null,1);


ALTER TABLE sub_module
CHANGE COLUMN group_type group_type ENUM('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config', 'buses', 'travels_log', 'billing') NULL DEFAULT NULL ;


INSERT INTO sub_module (id, name, module_id, group_type,created_by)
VALUES(68, 'app.billing',1,'billing',1);


INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by) VALUES
(162, '.pending', 'Facturación global',68 ,null,1);


INSERT INTO sub_module (id, name, module_id, group_type,created_by)
VALUES(69, 'app.billing',2,'billing',1);


INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by) VALUES
(163, '#list', 'Facturación ',69 ,null,1);