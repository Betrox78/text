-- 1614021853 DOWN cancel_parcels_G2102B596B4
update parcels set parcel_status = 9  where parcel_tracking_code = "G2102B596B4";
update parcels_packages set package_status = 9  where parcel_id = 27310;