-- 1652550514 DOWN add-key-customer-id-parcel-prepaid
ALTER TABLE parcels_prepaid
DROP FOREIGN  KEY   parcels_pp_customer_fk;


ALTER TABLE parcels_prepaid
DROP KEY   parcels_pp_customer_fk;