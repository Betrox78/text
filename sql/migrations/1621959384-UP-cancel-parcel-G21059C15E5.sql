-- 1621959384 UP cancel-parcel-G21059C15E5
update parcels set parcel_status = 4 where parcel_tracking_code = 'G21059C15E5';

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G21059C15E5");