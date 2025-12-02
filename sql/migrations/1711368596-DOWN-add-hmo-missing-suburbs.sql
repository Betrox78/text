-- 1711368596 DOWN add-hmo-missing-suburbs
DELETE FROM parcel_coverage where branchoffice_id in (select id from branchoffice where prefix = 'HMO01') AND created_at >= '2024-03-24';
DELETE FROM suburb where created_at >= '2024-03-24' and county_id = '19';
