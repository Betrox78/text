-- 1614275035 DOWN report-travel-seller-permissions
DELETE FROM permission WHERE id = (select id from sub_module where name = 'app.reports.travels_sellers');
DELETE FROM sub_module WHERE id= ((select id from sub_module where name = 'app.reports.travels_sellers'));