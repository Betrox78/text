-- 1606777598 UP permisos_bitacora_pos

INSERT INTO sub_module   (id,name,module_id,group_type,status,created_by) VALUES (101,"app.manifest_ead_rad",2,"parcel",1,1);
INSERT INTO permission (id, name, description, sub_module_id, created_by) VALUES
(213, '#list', 'Consultar', 101, 1),
(214, '#create', 'Crear',101, 1),
(215, '#cancel', 'Cancelar', 101, 1);
