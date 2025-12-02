-- 1701489748 DOWN shipments-trailers-feature-migrations
DROP TABLE trailers;

DROP TABLE shipments_trailers;

ALTER TABLE shipments_parcel_package_tracking
MODIFY COLUMN status ENUM('loaded', 'ready-to-go', 'in-transit', 'arrived-to-terminal', 'downloaded') NOT NULL,
DROP COLUMN trailer_id;