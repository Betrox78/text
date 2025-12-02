-- 1628885627 DOWN cancel_package_G2108432FE5_G2108A5E2DF

update parcels set parcel_status = 2 where parcel_tracking_code = 'G2108A5E2DF';

update parcels_packages set package_status = 2 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2108A5E2DF');

update parcels set parcel_status = 2 where parcel_tracking_code = 'G2108432FE5';

update parcels_packages set package_status = 2 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2108432FE5');