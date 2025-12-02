-- 1753039065 UP index parcel coverage
CREATE INDEX zip_code_idx ON parcel_coverage(zip_code);
CREATE INDEX branchoffice_zip_code_idx ON parcel_coverage(branchoffice_id, zip_code);