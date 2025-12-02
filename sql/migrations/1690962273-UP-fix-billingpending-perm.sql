-- 1690962273 UP fix-billingpending-perm
UPDATE `permission` SET `name` = '#list', `description` = 'Consultar' WHERE (`id` = '162');
INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `status`, `created_at`, `created_by`) VALUES ('140', 'app.billing.pending', '1', 'billing', '1', '2023-08-01 06:39:16', '1');
INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#generate', 'Generar factura global', '140', '0', '1', '2023-08-01 22:35:43', '1');
