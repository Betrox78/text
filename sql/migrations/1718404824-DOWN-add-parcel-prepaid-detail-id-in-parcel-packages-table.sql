-- 1718404824 DOWN add parcel prepaid detail id in parcel packages table
ALTER TABLE parcels_packages
DROP CONSTRAINT fk_parcels_packages_parcel_prepaid_detail_id,
DROP COLUMN parcel_prepaid_detail_id;