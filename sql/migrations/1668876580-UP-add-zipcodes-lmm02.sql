-- 1668876580 UP add zipcodes lmm02
INSERT INTO parcel_coverage(suburb_id, zip_code, branchoffice_id, created_by)
(SELECT pc.suburb_id, pc.zip_code, 8, 1 FROM parcel_coverage pc WHERE pc.branchoffice_id = 1);