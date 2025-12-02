-- 1630903452 UP cancel-parcel-G21094400C5-and-B2109FE74FF

update boarding_pass set boardingpass_status = 0 where reservation_code = "B2109FE74FF";

update customer set credit_available = (credit_available + 257.40) where id = 45131;