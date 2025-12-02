-- 1622158181 UP cancel-parcel-G21058E92FD
update parcels set parcel_status = 4 where parcel_tracking_code = 'G21058E92FD';

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G21058E92FD");