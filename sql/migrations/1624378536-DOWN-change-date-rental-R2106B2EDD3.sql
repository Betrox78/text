-- 1624378536 DOWN change-date-rental-R2106B2EDD3
update rental set created_at = "2021-06-22 13:50:45" where reservation_code = "R2106B2EDD3";

update tickets set created_at = "2021-06-22 13:50:45" , cash_out_id = 9900  where rental_id = (select id from rental where reservation_code = "R2106B2EDD3");

update tickets_details set created_at = "2021-06-22 13:50:45" where ticket_id = ( select id from tickets  where rental_id = (select id from rental where reservation_code = "R2106B2EDD3"));

update payment set created_at = "2021-06-22 13:50:45" where ticket_id = ( select id from tickets  where rental_id = (select id from rental where reservation_code = "R2106B2EDD3"));

update cash_out_move set created_at = "2021-06-22 13:50:45" , cash_out_id = 9900 where payment_id = (select id from  payment  where ticket_id = ( select id from tickets  where rental_id = (select id from rental where reservation_code = "R2106B2EDD3")));

update cash_out set total_on_register = (total_on_register + 14000) where id = 9900;

update cash_out set cash = (cash - 14000) , total_reported = (total_reported - 14000) , total_on_register = (total_on_register - 14000) where id = 9870;

update cash_out_detail set pieces = (pieces - 14) , accumulated = (accumulated - 14000) where id = 139238;

