-- 1744648912 UP add-parcels-packages-tracking-index
CREATE INDEX idx_parcels_tracking_parcel_action_created ON parcels_packages_tracking(parcel_id, action, created_at);