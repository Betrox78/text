-- 1743209321 UP add date colums parcels manifest table
ALTER TABLE parcels_manifest
ADD COLUMN num_route INTEGER DEFAULT NULL,
ADD COLUMN init_load_date DATETIME DEFAULT NULL,
ADD COLUMN finish_load_date DATETIME DEFAULT NULL,
ADD COLUMN init_route_date DATETIME DEFAULT NULL,
ADD COLUMN finish_route_date DATETIME DEFAULT NULL;