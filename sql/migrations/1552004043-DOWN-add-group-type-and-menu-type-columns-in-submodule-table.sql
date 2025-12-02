-- 1552004043 DOWN add group type and menu type columns in submodule table
ALTER TABLE sub_module
DROP COLUMN group_type,
DROP COLUMN menu_type;