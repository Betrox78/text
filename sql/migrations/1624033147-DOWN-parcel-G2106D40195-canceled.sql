-- 1624033147 DOWN parcel-G2106D40195-canceled
update parcels set parcel_status = 0 where parcel_tracking_code = 'G2106D40195';

update parcels_packages set package_status = 0 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2106D40195');