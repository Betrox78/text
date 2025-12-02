-- 1699526470 DOWN add submodule and permission ticket prices rules
DELETE FROM sub_module WHERE id = 144;
DELETE FROM permission WHERE sub_module_id = 144;