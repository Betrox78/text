-- 1756219630 UP add-index-for-po-bi-aug
CREATE INDEX payment_parcel_id_method_idx
  ON payment (parcel_id, payment_method_id);

CREATE INDEX parcels_created_at_status_idx
  ON parcels (created_at, parcel_status);