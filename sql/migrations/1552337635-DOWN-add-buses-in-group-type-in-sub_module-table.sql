-- 1552337635 DOWN add buses in group type in sub_module table
ALTER TABLE sub_module
MODIFY COLUMN group_type enum('general','admin','operation','logistic','vans','parcel','risks','reports','config') NOT NULL DEFAULT 'general' AFTER module_id,
MODIFY COLUMN menu_type enum('a_sub_catalogue','o_sub_catalogue','l_sub_config','v_sub_vansrentalcost','c_sub_generalconfig') DEFAULT NULL AFTER group_type;