-- 1731797644 UP add is_virtual in branchoffice table
ALTER TABLE branchoffice
ADD COLUMN is_virtual BOOLEAN NOT NULL DEFAULT FALSE AFTER transhipment_site_name;