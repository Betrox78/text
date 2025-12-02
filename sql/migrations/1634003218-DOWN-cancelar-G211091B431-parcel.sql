-- 1634003218 DOWN cancelar-G211091B431-parcel
update parcels set parcel_status = 9 where parcel_tracking_code = "G211091B431";

update parcels_packages set package_status = 9 where parcels_id = (select id from parcels where parcel_tracking_code = "G211091B431");
