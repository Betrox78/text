-- 1624483289 UP cancel-parcel-g21066561d1
update parcels set parcel_status = 4 where parcel_tracking_code = 'g21066561d1';

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = 'g21066561d1');