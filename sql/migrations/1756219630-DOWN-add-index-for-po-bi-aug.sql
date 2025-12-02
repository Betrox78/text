-- 1756219630 DOWN add-index-for-po-bi-aug
DROP INDEX payment_parcel_id_method_idx ON payment;
DROP INDEX parcels_created_at_status_idx ON parcels;