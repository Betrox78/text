-- 1552337635 UP add buses in group type in sub_module table
ALTER TABLE sub_module
MODIFY COLUMN group_type enum('general','admin','operation','logistic','vans','parcel','risks','reports','config','buses') NOT NULL DEFAULT 'general' AFTER module_id,
MODIFY COLUMN menu_type enum('a_sub_catalogue','o_sub_catalogue','l_sub_config','v_sub_vansrentalcost','c_sub_generalconfig', 'v_sub_vans', 'p_sub_parcel') DEFAULT NULL AFTER group_type;