-- 1635232465 DOWN cancelar-parcel-G21105999DA
update parcels set parcel_status = 1 where parcel_tracking_code = "G21105999DA";

update parcels_packages set package_status = 1 where parcel_id = (select id from parcels where parcel_tracking_code = "G21105999DA");