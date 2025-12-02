-- 1653601632 DOWN alter-prepaid-column-name
alter table payment
change column parcel_prepaid_id parcels_prepaid_id  int(11) DEFAULT NULL;