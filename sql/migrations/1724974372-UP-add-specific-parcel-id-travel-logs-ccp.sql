-- 1724974372 UP add-specific-parcel-id-travel-logs-ccp
ALTER TABLE travel_logs_ccp
ADD COLUMN specific_parcel_id INT DEFAULT NULL,
ADD CONSTRAINT fk_travel_logs_ccp_parcels_id FOREIGN KEY (specific_parcel_id) REFERENCES parcels(id);