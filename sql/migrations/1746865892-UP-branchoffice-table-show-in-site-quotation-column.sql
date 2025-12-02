-- 1746865892 UP branchoffice table show in site quotation column
ALTER TABLE branchoffice
RENAME COLUMN show_in_site TO show_in_site_terminals,
ADD COLUMN show_in_site_quotation BOOLEAN DEFAULT TRUE AFTER is_virtual;