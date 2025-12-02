-- 1695752652 UP add-parcel-acummulated-sellers-perms
INSERT INTO `sub_module` (`id`, `name`, `module_id`, `group_type`, `menu_type`, `status`, `created_at`, `created_by`) VALUES (143, 'app.reports.parcel_sellers', '1', 'reports', 'r_sub_reportsales', '1', '2023-09-26 03:28:50', '1');
INSERT INTO `permission` (`name`, `description`, `sub_module_id`, `multiple`, `status`, `created_at`, `created_by`) VALUES ('#list', 'Ver reporte', '143', '0', '1', '2023-09-26 22:35:43', '1');
