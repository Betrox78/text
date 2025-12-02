-- 1558978743 UP cancel parcel permission


INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(70, 'app.parcel.cancellation', 2, 'parcel', 'p_sub_parcel',   1);


INSERT into permission (id, name, description, sub_module_id,dependency_id,created_by) VALUES
(164,'#cancel', 'Cancelación de paquetería',70 ,null,1);