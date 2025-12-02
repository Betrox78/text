-- 1621008810 DOWN cancel-parcel-G21057986D7

update parcels set parcel_status = 2 where parcel_tracking_code = 'G21057986D7';

update parcels_packages set package_status = 2 where parcel_id = (select id from parcels where parcel_tracking_code = "G21057986D7");