-- 1613580983 UP parcel-g2101f70666-to-delivered
update parcels set parcel_status = 2 where id =  (select id from (select id from parcels where parcel_tracking_code = "G2101F70666") as x);

update parcels_packages set package_status = 2 where parcel_id = (select id from (select id from parcels where parcel_tracking_code = "G2101F70666") as x);