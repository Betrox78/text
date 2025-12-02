-- 1653601632 UP alter-prepaid-column-name
alter table payment
change column parcels_prepaid_id parcel_prepaid_id int(11) DEFAULT NULL;