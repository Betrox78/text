-- 1755695698 DOWN add-cfdi-module-perms
DELETE FROM permission WHERE sub_module_id = 161;
DELETE FROM sub_module WHERE id = 161;