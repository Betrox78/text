-- 1758161704 DOWN add contingency finish route date in parcels manifest table
ALTER TABLE parcels_manifest
DROP COLUMN contingency_finish_route_date;