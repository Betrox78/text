-- 1552004043 UP add group type and menu type columns in submodule table
ALTER TABLE sub_module
ADD COLUMN group_type enum('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config') NOT NULL DEFAULT 'general' AFTER module_id,
ADD COLUMN menu_type enum('a_sub_catalogue', 'o_sub_catalogue', 'l_sub_config', 'v_sub_vansrentalcost', 'c_sub_generalconfig') DEFAULT NULL AFTER group_type;