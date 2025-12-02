-- 1633494014 UP cancel-G2110BAC12D-parcel
update parcels set parcel_status = 4 where parcel_tracking_code = "G2110BAC12D";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G2110BAC12D");

update customer set credit_available = (credit_available + 76.99 ) where id = (select customer_id from parcels where parcel_tracking_code = "G2110BAC12D");