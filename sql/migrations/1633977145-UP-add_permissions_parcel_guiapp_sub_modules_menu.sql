-- 1633977145 UP add_permissions_parcel_guiapp_sub_modules_menu
ALTER TABLE `sub_module`
CHANGE COLUMN `menu_type` `menu_type` ENUM('a_sub_catalogue', 'o_sub_catalogue', 'l_sub_config', 'v_sub_vansrentalcost', 'c_sub_generalconfig', 'v_sub_vans', 'p_sub_parcel', 'r_sub_reports', 'r_sub_reportsales', 'r_sub_reportinventory', 'r_sub_reportcashouts', 'r_sub_reportlogistics', 'r_sub_reportothers', 'g_sub_guia_pp') NULL DEFAULT NULL ;
ALTER TABLE `sub_module`
CHANGE COLUMN `group_type` `group_type` ENUM('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config', 'buses', 'travels_log', 'billing', 'guia_pp') NULL DEFAULT NULL ;

INSERT INTO sub_module   (id,name,module_id,group_type,menu_type,status,created_by) VALUES (108,"app.parcel_guiapp.report",2,"guia_pp",'g_sub_guia_pp',1,1);
INSERT INTO sub_module   (id,name,module_id,group_type,menu_type,status,created_by) VALUES (109,"app.parcel_guiapp.cancellation",2,"guia_pp",'g_sub_guia_pp',1,1);
INSERT INTO sub_module   (id,name,module_id,group_type,menu_type,status,created_by) VALUES (110,"app.parcel_guiapp.delivery",2,"guia_pp",'g_sub_guia_pp',1,1);
INSERT INTO sub_module   (id,name,module_id,group_type,menu_type,status,created_by) VALUES (111,"app.parcel_guiapp.details",2,"guia_pp",'g_sub_guia_pp',1,1);
INSERT INTO sub_module   (id,name,module_id,group_type,menu_type,status,created_by) VALUES (112,"app.parcel_guiapp.reception",2,"guia_pp",'g_sub_guia_pp',1,1);

INSERT INTO permission (id, name, description, sub_module_id, created_by) VALUES
(230, '#report', 'Ver reporte', 108, 1),

(231, '#cancel.return', 'Devolución', 109, 1),
(232, '#cancel.rework', 'Reexpedición', 109, 1),
(233, '#cancel.end', 'Cancelación extemporánea', 109, 1),
(234, '#Cancel.fast', 'Cancelacion al momento',109, 1),

(235, '#delivery', 'Entrega de paquetería', 111, 1),


(236, '#print', 'Re-impresiones', 111, 1),
(237, '#cancel', 'Cancelar', 111, 1),
(238, '#detail', 'Ver detalle de cartas porte', 111, 1),

(239, '#create', 'Crear', 112, 1);