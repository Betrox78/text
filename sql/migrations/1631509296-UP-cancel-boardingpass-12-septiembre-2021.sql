-- 1631509296 UP cancel-boardingpass-12-septiembre-2021
update boarding_pass set boardingpass_status = 0  where reservation_code = "B21091031D1";

update customer set credit_available = (credit_available + 531.60) where id = 45131;


update parcels set parcel_status = 4 where parcel_tracking_code = 'G210959723C';

