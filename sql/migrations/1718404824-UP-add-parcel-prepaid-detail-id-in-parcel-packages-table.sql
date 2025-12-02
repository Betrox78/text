-- 1718404824 UP add parcel prepaid detail id in parcel packages table
ALTER TABLE parcels_packages
ADD COLUMN parcel_prepaid_detail_id INT DEFAULT NULL,
ADD CONSTRAINT fk_parcels_packages_parcel_prepaid_detail_id FOREIGN KEY(parcel_prepaid_detail_id)
	REFERENCES parcels_prepaid_detail(id) ON UPDATE NO ACTION ON DELETE NO ACTION;