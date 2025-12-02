-- 1750921548 DOWN add permission ops parcels manifest
DELETE FROM permission WHERE sub_module_id = 157;
DELETE FROM sub_module WHERE id = 157;