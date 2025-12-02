-- 1696293426 UP parcels-db-indexes
CREATE INDEX parcels_parcels_status_created_at_idx ON parcels(parcel_status, created_at);
CREATE INDEX parcels_prepaid_parcels_status_created_at_idx ON parcels_prepaid(parcel_status, created_at);
CREATE INDEX parcels_prepaid_parcels_status_idx ON parcels_prepaid(parcel_status);
CREATE INDEX parcels_prepaid_created_at_idx ON parcels_prepaid(created_at);