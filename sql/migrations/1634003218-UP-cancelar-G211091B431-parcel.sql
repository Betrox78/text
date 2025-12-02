-- 1634003218 UP cancelar-G211091B431-parcel
update parcels set parcel_status = 4 where parcel_tracking_code = "G211091B431";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G211091B431");
