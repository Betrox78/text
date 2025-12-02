-- 1761016413 UP add-assign-cbi-to-service-perm
INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `status`, `created_at`, `created_by`) VALUES (162, 'app.billing.assign_cbi', '1', 'billing', '1', '2025-10-24 22:30:32', '1');

INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#assign', 'Asignar RFC', '162', '0', '1', '2025-10-24 22:30:32', '1');
INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#re_assign', 'Re-asignar RFC', '162', '0', '1', '2025-10-24 22:30:32', '1');
