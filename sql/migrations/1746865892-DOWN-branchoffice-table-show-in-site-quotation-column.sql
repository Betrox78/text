-- 1746865892 DOWN branchoffice table show in site quotation column
ALTER TABLE branchoffice
RENAME COLUMN show_in_site_terminals TO show_in_site,
DROP COLUMN show_in_site_quotation;