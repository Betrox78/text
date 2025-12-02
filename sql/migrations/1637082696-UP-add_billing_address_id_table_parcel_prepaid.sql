-- 1637082696 UP add_billing_address_id_table_parcel_prepaid

ALTER TABLE parcels_prepaid
ADD COLUMN billing_address_id int(11) DEFAULT NULL;