-- 1633494014 DOWN cancel-G2110BAC12D-parcel
update parcels set parcel_status = 11 where parcel_tracking_code = "G210953E3AC";

update parcels_packages set package_status = 11 where parcels_id = (select id from parcels where parcel_tracking_code = "G210953E3AC");

update customer set credit_available = (credit_available - 76.99 ) where id = (select customer_id from parcels where parcel_tracking_code = "G210953E3AC");