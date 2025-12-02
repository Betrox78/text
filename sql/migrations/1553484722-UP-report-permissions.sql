-- 1553484722 UP report-permissions
ALTER TABLE sub_module
MODIFY COLUMN menu_type enum('a_sub_catalogue','o_sub_catalogue','l_sub_config','v_sub_vansrentalcost','c_sub_generalconfig','v_sub_vans','p_sub_parcel', 'r_sub_reports') DEFAULT NULL;

INSERT INTO sub_module (id, name, module_id, group_type, menu_type, created_by) VALUES
(50, 'app.reports.inventory', 1, 'reports', 'r_sub_reports', 1),
(51, 'app.reports.sales', 1, 'reports', 'r_sub_reports', 1);

INSERT INTO permission (id,name, description, dependency_id, sub_module_id, created_by) VALUES
(135,'#list','Ver reporte', null,  50, 1),
(136,'#list','Ver reporte', null, 51, 1);