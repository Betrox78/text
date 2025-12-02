-- 1615225959 UP cancel-parcel-G2012BA29E7

update parcels set parcel_status = 4 where parcel_tracking_code = "G2012BA29E7";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G2012BA29E7");