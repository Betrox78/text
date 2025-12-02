-- 1758161704 UP add contingency finish route date in parcels manifest table
ALTER TABLE parcels_manifest
ADD COLUMN contingency_finish_route_date DATETIME DEFAULT NULL;