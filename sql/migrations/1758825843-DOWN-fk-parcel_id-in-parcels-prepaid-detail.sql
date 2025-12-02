-- 1758825843 DOWN fk parcel_id in parcels prepaid detail
ALTER TABLE parcels_prepaid_detail
DROP FOREIGN KEY fk_parcels_prepaid_detail_parcel;