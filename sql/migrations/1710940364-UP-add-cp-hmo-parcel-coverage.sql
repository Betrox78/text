-- 1710940364 UP add-cp-hmo-parcel-coverage
INSERT INTO parcel_coverage (suburb_id, zip_code, branchoffice_id, created_by)
SELECT DISTINCT
  sub.id,
  sub.zip_code,
  (SELECT id FROM branchoffice WHERE prefix = 'HMO01') AS branchoffice_id,
  1
FROM suburb sub
LEFT JOIN branchoffice b ON sub.id = b.suburb_id
WHERE sub.county_id = (SELECT county_id FROM branchoffice WHERE prefix = 'HMO01')
  AND sub.zip_code NOT IN (83144, 83160)
  AND sub.suburb_type <> 'Ranchería'
GROUP BY sub.id, sub.zip_code
ORDER BY sub.id, sub.zip_code DESC;