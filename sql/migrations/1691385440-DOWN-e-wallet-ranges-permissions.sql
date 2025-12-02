-- 1691385440 DOWN e-wallet-ranges-permissions
ALTER TABLE `sub_module`
CHANGE COLUMN `group_type` `group_type` ENUM('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config', 'buses', 'travels_log', 'billing', 'guia_pp', 'prepaid') NULL DEFAULT NULL ;

DELETE FROM permission WHERE sub_module_id = 141;
DELETE FROM sub_module WHERE id = 141;
