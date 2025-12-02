-- 1601398922 UP insert_permisos_bitacora_rad_ead

INSERT INTO sub_module   (id,name,module_id,group_type,status,created_by) VALUES (99,"app.manifest_ead_rad",1,"parcel",1,1);
INSERT INTO permission (id, name, description, sub_module_id, created_by) VALUES
(208, '#list', 'Consultar', 99, 1),
(209, '#create', 'Crear',99, 1),
(210, '#cancel', 'Cancelar', 99, 1);