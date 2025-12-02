-- 1743209321 DOWN add date colums parcels manifest table
ALTER TABLE parcels_manifest
DROP COLUMN num_route,
DROP COLUMN init_load_date,
DROP COLUMN finish_load_date,
DROP COLUMN init_route_date,
DROP COLUMN finish_route_date;