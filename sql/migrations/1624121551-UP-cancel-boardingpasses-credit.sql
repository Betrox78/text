-- 1624121551 UP cancel-boardingpasses-credit

update boarding_pass set boardingpass_status = 0 , debt = 0.00 where reservation_code = "B210644CBCD";
update customer set credit_available = 900.00 where id = (select customer_id from boarding_pass where reservation_code = "B210644CBCD");


update boarding_pass set boardingpass_status = 0 , debt = 0.00 where reservation_code = "B21060B059A";
update boarding_pass set boardingpass_status = 0 , debt = 0.00 where reservation_code = "B2106A06B12";
update customer set credit_available = (credit_available + 86.7) where id = (select customer_id from boarding_pass where reservation_code = "B21060B059A");

