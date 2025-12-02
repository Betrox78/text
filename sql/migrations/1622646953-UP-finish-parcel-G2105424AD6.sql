-- 1622646953 UP cancel-parcel-G2102AE3553
update parcels set parcel_status = 2 where parcel_tracking_code = 'G2105424AD6';

update parcels_packages set package_status = 2 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2105424AD6');