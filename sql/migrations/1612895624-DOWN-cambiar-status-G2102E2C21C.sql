-- 1612895624 DOWN cambiar-status-G2102E2C21C
update parcels set parcel_status = 2 where parcel_tracking_code = "G2102E2C21C";

update parcels_packages set package_status = 2 where parcel_id = (select id from parcels where parcel_tracking_code = "G2102E2C21C");