-- 1691385440 UP e-wallet-ranges-permissions
ALTER TABLE `sub_module`
CHANGE COLUMN `group_type` `group_type` ENUM('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config', 'buses', 'travels_log', 'billing', 'guia_pp', 'prepaid', 'e-wallet') NULL DEFAULT NULL ;

INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `status`, `created_at`, `created_by`) VALUES ('141', 'app.e_wallet_prices_range', '1', 'e-wallet', '1', '2023-08-05 03:28:50', '1');

INSERT INTO `permission` (`id`, `name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('284', '#list', 'Ver listado', '141', '0', '1', '2023-08-01 22:35:43', '1');
INSERT INTO `permission` (`id`, `name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('285', '#add', 'Registrar', '284', '141', '0', '1', '2023-08-01 22:35:43', '1');
INSERT INTO `permission` (`id`, `name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('286', '#edit', 'Modificar', '284', '141', '0', '1', '2023-08-01 22:35:43', '1');
INSERT INTO `permission` (`id`, `name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('287', '#delete', 'Eliminar', '284', '141', '0', '1', '2023-08-01 22:35:43', '1');
