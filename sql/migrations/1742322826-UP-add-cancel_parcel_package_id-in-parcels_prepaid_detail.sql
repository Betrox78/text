-- 1742322826 UP add cancel_parcel_package_id in parcels_prepaid_detail
ALTER TABLE parcels_prepaid_detail
ADD COLUMN cancel_parcel_package_id INT DEFAULT NULL,
ADD CONSTRAINT parcels_prepaid_detail_cancel_parcel_package_id
	FOREIGN KEY (cancel_parcel_package_id) REFERENCES parcels_packages(id) ON UPDATE NO ACTION ON DELETE NO ACTION;