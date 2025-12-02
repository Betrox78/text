-- 1629925688 DOWN cancel-boardingpasses-agosto-25-2021
update parcels set parcel_status = 2 where parcel_tracking_code = "G21080393CC";
update customer set credit_available = (credit_available - 76.99) where id =  (select customer_id from parcels where parcel_tracking_code = "G21080393CC");
update parcels_packages set package_status = 2 where parcel_id = (select  id from parcels where parcel_tracking_code = "G21080393CC");

update parcels set parcel_status = 2 where parcel_tracking_code = "G21084D400A";
update customer set credit_available = (credit_available - 76.99) where id =  (select customer_id from parcels where parcel_tracking_code = "G21084D400A");
update parcels_packages set package_status = 2 where parcel_id = (select  id from parcels where parcel_tracking_code = "G21084D400A");

update parcels set parcel_status = 2 where parcel_tracking_code = "G2108C33CE3";
update customer set credit_available = (credit_available - 76.99) where id =  (select customer_id from parcels where parcel_tracking_code = "G2108C33CE3");
update parcels_packages set package_status = 2 where parcel_id = (select  id from parcels where parcel_tracking_code = "G2108C33CE3");