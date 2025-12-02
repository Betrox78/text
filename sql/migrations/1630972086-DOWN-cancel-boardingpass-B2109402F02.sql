-- 1630972086 DOWN cancel-boardingpass-B2109402F02

update boarding_pass set boardingpass_status = 1 where reservation_code = "B2109402F02";

update customer set credit_available = (credit_available - 531.60) where id = 45131;