-- 1624121551 DOWN cancel-boardingpasses-credit
update boarding_pass set boardingpass_status = 3 , debt = 43.35 where reservation_code = "B210644CBCD";
update customer set credit_available = 856.65 where id = (select customer_id from boarding_pass where reservation_code = "B210644CBCD");


update boarding_pass set boardingpass_status = 3 , debt = 0 where reservation_code = "B21060B059A";
update boarding_pass set boardingpass_status = 3 , debt = 43.35 where reservation_code = "B2106A06B12";
update customer set credit_available = ( credit_available - 43.35) where id = (select customer_id from boarding_pass where reservation_code = "B21060B059A");