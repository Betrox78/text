-- 1746826951 UP branchoffice table show in site column
ALTER TABLE branchoffice
ADD COLUMN show_in_site BOOLEAN DEFAULT TRUE AFTER is_virtual;