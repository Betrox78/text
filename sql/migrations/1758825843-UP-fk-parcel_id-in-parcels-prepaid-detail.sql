-- 1758825843 UP fk parcel_id in parcels prepaid detail
ALTER TABLE parcels_prepaid_detail
ADD CONSTRAINT fk_parcels_prepaid_detail_parcel
	FOREIGN KEY (parcel_id) REFERENCES parcels(id) ON DELETE CASCADE ON UPDATE CASCADE;