-- 1623518927 UP cancel_parcels_G210692AF1D_G210658D424
update parcels set parcel_status = 4  where parcel_tracking_code = "G210692AF1D" or parcel_tracking_code = "G210658D424" ;
update parcels_packages set package_status = 4  where parcel_id = 43394  or parcel_id = 43389;