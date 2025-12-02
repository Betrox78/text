-- 1587142787 UP parcels-add-indexes
CREATE INDEX parcels_status_idx ON parcels(status);
CREATE INDEX parcels_parcel_status_idx ON parcels(parcel_status);
CREATE INDEX parcels_parcel_tracking_code_idx ON parcels(parcel_tracking_code);
CREATE INDEX parcels_delivered_at_idx ON parcels(delivered_at);
CREATE INDEX parcels_created_at_idx ON parcels(created_at);
CREATE INDEX parcels_canceled_at_idx ON parcels(canceled_at);
CREATE INDEX parcels_payment_condition_idx ON parcels(payment_condition);
CREATE INDEX parcels_purchase_origin_idx ON parcels(purchase_origin);