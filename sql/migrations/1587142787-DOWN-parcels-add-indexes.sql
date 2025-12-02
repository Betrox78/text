-- 1587142787 DOWN parcels-add-indexes
DROP INDEX parcels_status_idx ON parcels;
DROP INDEX parcels_parcel_status_idx ON parcels;
DROP INDEX parcels_parcel_tracking_code_idx ON parcels;
DROP INDEX parcels_delivered_at_idx ON parcels;
DROP INDEX parcels_created_at_idx ON parcels;
DROP INDEX parcels_canceled_at_idx ON parcels;
DROP INDEX parcels_payment_condition_idx ON parcels;
DROP INDEX parcels_purchase_origin_idx ON parcels;