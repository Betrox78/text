-- 1636387201 DOWN modify-columns-prepaid-detail
ALTER TABLE parcels_prepaid_detail
MODIFY COLUMN status NOT NULL DEFAULT '1',
ADD COLUMN billing_address_id DEFAULT NULL,
MODIFY COLUMN  parcel_status NOT NULL DEFAULT '0'
;


ALTER TABLE parcels_prepaid
MODIFY COLUMN `debt` float(12,2) DEFAULT '0.00';