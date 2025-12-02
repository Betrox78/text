-- 1558562709 DOWN add apps permissions
DELETE FROM module WHERE name = 'app_operation' OR name = 'app_driver';

DELETE FROM sub_module WHERE name = 'all' AND (module_id = 4 OR module_id = 5);