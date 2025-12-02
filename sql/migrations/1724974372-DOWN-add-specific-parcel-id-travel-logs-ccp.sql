-- 1724974372 DOWN add-specific-parcel-id-travel-logs-ccp
ALTER TABLE travel_logs_ccp
DROP CONSTRAINT fk_travel_logs_ccp_parcels_id,
DROP COLUMN specific_parcel_id;