-- 1627692344 DOWN cancel_package_G2107EBE36C_G2107784095


update parcels set parcel_status = 0 where parcel_tracking_code = 'G2107EBE36C';
update parcels_packages set package_status = 0 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2107EBE36C');

update parcels set parcel_status = 0 where parcel_tracking_code = 'G2107784095';
update parcels_packages set package_status = 0 where parcel_id = (select id from parcels where parcel_tracking_code = 'G2107784095');