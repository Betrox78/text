-- 1691567060 UP sellers-permissions
INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `menu_type`, `status`, `created_at`, `created_by`) VALUES ('142', 'app.sellers', '1', 'admin', 'a_sub_catalogue', '1', '2023-08-09 03:28:50', '1');

INSERT INTO `permission` (`id`, `name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('288', '#list', 'Ver listado', '142', '0', '1', '2023-08-09 22:35:43', '1');
INSERT INTO `permission` (`id`, `name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('289', '#add', 'Registrar', '288', '142', '0', '1', '2023-08-09 22:35:43', '1');
INSERT INTO `permission` (`id`, `name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('290', '#edit', 'Modificar', '288', '142', '0', '1', '2023-08-09 22:35:43', '1');
INSERT INTO `permission` (`id`, `name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('291', '#delete', 'Eliminar', '288', '142', '0', '1', '2023-08-09 22:35:43', '1');
