-- 1737657484 UP add-parcel-tracking-code-index-length3
CREATE INDEX idx_parcels_tracking_prefix_3 ON parcels(parcel_tracking_code(3));
