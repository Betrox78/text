-- 1619223591 UP cancel-parcel-G2104402834-and-G2104539E69
update parcels set parcel_status = 4  where parcel_tracking_code = "G2104402834";
update parcels_packages set package_status = 4  where parcel_id = (select id from parcels where parcel_tracking_code = "G2104402834");

update parcels set parcel_status = 4  where parcel_tracking_code = "G2104539E69";
update parcels_packages set package_status = 4  where parcel_id = (select id from parcels where parcel_tracking_code = "G2104539E69");