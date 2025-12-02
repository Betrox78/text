-- 1688191800 DOWN
DELETE FROM sub_module WHERE id = 139 AND name = 'app.reports.prepaid_sales';

DELETE FROM permission WHERE sub_module_id = 139 AND description = 'Ventas boletos prepago';