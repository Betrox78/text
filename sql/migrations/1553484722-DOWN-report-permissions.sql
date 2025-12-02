-- 1553484722 DOWN report-permissions
DELETE FROM permission WHERE id=135 OR id=136;
DELETE FROM sub_module WHERE id=50 OR id=51;

ALTER TABLE sub_module
MODIFY COLUMN menu_type enum('a_sub_catalogue','o_sub_catalogue','l_sub_config','v_sub_vansrentalcost','c_sub_generalconfig','v_sub_vans','p_sub_parcel') DEFAULT NULL;

