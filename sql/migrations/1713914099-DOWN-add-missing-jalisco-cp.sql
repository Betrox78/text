-- 1713914099 DOWN add-missing-jalisco-cp
DELETE FROM parcel_coverage where branchoffice_id in (select id from branchoffice where prefix = 'GDL01') AND created_at >= '2024-04-23';
DELETE FROM suburb where created_at >= '2024-04-23' and county_id in (1533, 1534, 1540, 1544);