-- 1635315777 UP cancelar-parcel-

update parcels set parcel_status = 4 where parcel_tracking_code = "G2110B6FD39" and customer_id = 17014;

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G2110B6FD39" and customer_id = 17014);