-- 1621008810 UP cancel-parcel-G21057986D7

update parcels set parcel_status = 4 where parcel_tracking_code = 'G21057986D7';

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G21057986D7");