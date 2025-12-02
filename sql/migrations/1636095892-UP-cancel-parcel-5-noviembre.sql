-- 1636095892 UP cancel-parcel-5-noviembre
update parcels set parcel_status = 4 where parcel_tracking_code = "G2111F38BCD";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G2111F38BCD");
update parcels set parcel_status = 4 where parcel_tracking_code = "G2111C7B49B";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G2111C7B49B");

