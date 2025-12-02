-- 1665420879 UP insert-submodule-debt-account-status-report
insert into sub_module(id, name, module_id, group_type, menu_type, status, created_by) values
(128, 'app.reports.debt_account_status', 1, 'reports', 'r_sub_reportsales', 1, 1);

insert into permission(id, name, description, sub_module_id, multiple, status, created_by) values
(260, '#list', 'Ver reporte', 128, 0, 1, 1);