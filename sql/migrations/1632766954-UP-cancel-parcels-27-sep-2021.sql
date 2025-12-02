-- 1632766954 UP cancel-parcels-27-sep-2021
update parcels set parcel_status = 4 where parcel_tracking_code = "G210953E3AC";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G210953E3AC");

update customer set credit_available = (credit_available + 76.99 ) where id = (select customer_id from parcels where parcel_tracking_code = "G210953E3AC");

update parcels set parcel_status = 4 where parcel_tracking_code = "G2109D3F66F";

update parcels_packages set package_status = 4 where parcel_id = (select id from parcels where parcel_tracking_code = "G2109D3F66F");