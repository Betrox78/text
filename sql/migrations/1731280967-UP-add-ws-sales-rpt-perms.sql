-- 1731280967 UP add-ws-sales-rpt-perms
INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `menu_type`, `status`, `created_at`, `created_by`) VALUES (152, 'app.reports.parcel_ws', '1', 'reports', 'r_sub_reportsales', '1', '2024-11-10 03:28:50', '1');
INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#list', 'Ver reporte', '152', '0', '1', '2024-11-10 03:28:50', '1');
