-- 1711988187 DOWN add-travel-logs-cost-rpt-perm
DELETE FROM permission WHERE sub_module_id = (select id from sub_module where name = 'app.reports.travel_logs_cost');
DELETE FROM sub_module WHERE name = 'app.reports.travel_logs_cost' and id > 0;


