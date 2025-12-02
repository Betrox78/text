-- 1732384023 UP create-segments-permissions

INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `menu_type`, `status`, `created_at`, `created_by`) VALUES ('153', 'app.segments', '1', 'admin', 'a_sub_catalogue', '1', '2024-11-11 03:28:50', '1');

INSERT INTO `permission` (`id`, `name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('302', '#list', 'Ver listado', '153', '0', '1', '2023-08-09 22:35:43', '1');
INSERT INTO `permission` (`name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#add', 'Registrar', '302', '153', '0', '1', '2024-11-11 22:35:43', '1');
INSERT INTO `permission` (`name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#edit', 'Modificar', '302', '153', '0', '1', '2024-11-11 22:35:43', '1');
INSERT INTO `permission` (`name`, `description`, `dependency_id`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#delete', 'Eliminar', '302', '153', '0', '1', '2024-11-11 22:35:43', '1');