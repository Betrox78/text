-- 1631992672 UP cancel_parcel_G2109E81EC7_G2109B55A0D_G2109FAEAF0


update parcels set parcel_status = 4 where parcel_tracking_code = 'G2109E81EC7';

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2109E81EC7');

update parcels set parcel_status = 4 where parcel_tracking_code = 'G2109B55A0D';

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2109B55A0D');

update parcels set parcel_status = 4 where parcel_tracking_code = 'G2109FAEAF0';

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2109FAEAF0');