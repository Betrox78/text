-- 1573688275 DOWN add-menu-types-to-submodules
DELETE FROM sub_module WHERE id BETWEEN 80 AND 94;
DELETE FROM permission WHERE id BETWEEN 182 AND 196;
UPDATE sub_module SET menu_type = 'r_sub_reports' WHERE id IN (50, 51, 53, 54, 55, 66, 67, 77, 78, 79);
ALTER TABLE  sub_module MODIFY COLUMN menu_type  ENUM('a_sub_catalogue', 'o_sub_catalogue', 'l_sub_config',
    'v_sub_vansrentalcost', 'c_sub_generalconfig', 'v_sub_vans', 'p_sub_parcel', 'r_sub_reports') NULL;