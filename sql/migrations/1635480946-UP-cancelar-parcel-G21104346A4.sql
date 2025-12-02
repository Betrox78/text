-- 1635480946 UP cancelar-parcel-G21104346A4
update parcels set parcel_status = 4 where parcel_tracking_code = "G21104346A4" and customer_id = 23014;

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G21104346A4" and customer_id = 23014);