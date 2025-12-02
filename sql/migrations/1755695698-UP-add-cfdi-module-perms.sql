-- 1755695698 UP add-cfdi-module-perms
INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `status`, `created_at`, `created_by`) VALUES (161, 'app.billing.cfdi', '1', 'billing', '1', '2025-08-10 06:35:15', '1');
INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#list', 'Consultar CFDI', '161', '0', '1', '2025-08-10 22:22:44', '1');

-- aún no se necesitan crear, pero tengamoslo listos
INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#credit_note', 'Generar notas de credito', '161', '0', '1', '2025-08-10 06:39:16', '1');
INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#payment_complement', 'Generar complementos de pago', '161', '0', '1', '2025-08-10 06:39:16', '1');
