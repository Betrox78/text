-- 1735277970 DOWN origin contingency permissions
DELETE FROM permission WHERE sub_module_id = 154;
DELETE FROM sub_module WHERE id = 154;