-- 1632424235 DOWN cancel-parcels-23-09-2021
update parcels set parcel_status = 0 where parcel_tracking_code = 'G210957D7B3';

update parcels set parcel_status = 11 where parcel_tracking_code = 'G210996F38D';

update customer set credit_available = (credit_available - 153.98) where id = (select customer_id from parcels where parcel_tracking_code= "G210957D7B3" );
