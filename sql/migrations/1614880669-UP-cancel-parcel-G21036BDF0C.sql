-- 1614880669 UP cancel-parcel-G21036BDF0C
update parcels set parcel_status = 4 where parcel_tracking_code = "G21036BDF0C";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G21036BDF0C");