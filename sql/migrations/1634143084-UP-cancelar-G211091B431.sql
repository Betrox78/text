-- 1634143084 UP cancelar-G211091B431
update parcels set parcel_status = 4 where parcel_tracking_code = "G21105AE19B" and customer_id = 53367;

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G21105AE19B" and customer_id = 53367);