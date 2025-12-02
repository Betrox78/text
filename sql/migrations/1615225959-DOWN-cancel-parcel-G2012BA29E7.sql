-- 1615225959 DOWN cancel-parcel-G2012BA29E7

update parcels set parcel_status = 9 where parcel_tracking_code = "G2012BA29E7";

update parcels_packages set package_status = 2 where parcel_id = (select id from parcels where parcel_tracking_code = "G2012BA29E7");