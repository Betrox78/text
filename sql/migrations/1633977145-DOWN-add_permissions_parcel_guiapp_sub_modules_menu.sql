-- 1633977145 DOWN add_permissions_parcel_guiapp_sub_modules_menu
DELETE FROM `permission` WHERE (`id` = '233');
DELETE FROM `permission` WHERE (`id` = '232');
DELETE FROM `permission` WHERE (`id` = '231');
DELETE FROM `permission` WHERE (`id` = '230');
DELETE FROM `permission` WHERE (`id` = '229');
DELETE FROM `permission` WHERE (`id` = '228');
DELETE FROM `permission` WHERE (`id` = '227');
DELETE FROM `permission` WHERE (`id` = '226');
DELETE FROM `permission` WHERE (`id` = '225');
DELETE FROM `permission` WHERE (`id` = '224');

DELETE FROM `sub_module` WHERE (`id` = '110');
DELETE FROM `sub_module` WHERE (`id` = '109');
DELETE FROM `sub_module` WHERE (`id` = '108');
DELETE FROM `sub_module` WHERE (`id` = '107');
DELETE FROM `sub_module` WHERE (`id` = '106');

ALTER TABLE `sub_module`
CHANGE COLUMN `menu_type` `menu_type` ENUM('a_sub_catalogue', 'o_sub_catalogue', 'l_sub_config', 'v_sub_vansrentalcost', 'c_sub_generalconfig', 'v_sub_vans', 'p_sub_parcel', 'r_sub_reports', 'r_sub_reportsales', 'r_sub_reportinventory', 'r_sub_reportcashouts', 'r_sub_reportlogistics', 'r_sub_reportothers') NULL DEFAULT NULL ;
ALTER TABLE `sub_module`
CHANGE COLUMN `group_type` `group_type` ENUM('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config', 'buses', 'travels_log', 'billing') NULL DEFAULT NULL ;


