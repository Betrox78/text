-- 1724699044 UP add-uuid-travel-logs-ccp
ALTER TABLE travel_logs_ccp
ADD COLUMN uuid VARCHAR(50) NULL;

ALTER TABLE parcels_manifest_ccp
ADD COLUMN uuid VARCHAR(50) NULL;