-- 1625789769 DOWN cancel-parcel-G2107E512F
update parcels set parcel_status = 2 where parcel_tracking_code = 'G21073E512F';

update parcels_packages set package_status = 2 where parcel_id = (select id from parcels where parcel_tracking_code = 'G21073E512F');