
UPDATE rental set kilometers_end = 156032 , rent_status = 3 , penalty_days = 0 , received_at = "2021-04-04 20:32:00" , received_by = (select id from users where email = "igalaviz@allabordo.com")    where reservation_code = "R21028C3867" ;

UPDATE rental set payment_status = '1' where reservation_code = "R21028C3867" ;


-- INSERT CHECKLIST

update rental_checklist set status_on_reception = 1 where rental_id = ( select id from rental where reservation_code = "R21028C3867");

-- INSERT RETURN DE GARANTIA

insert into tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code )
VALUES (
 (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1)
, (select id from rental where reservation_code = "R21028C3867")
, 0
, 5000
, 5000
, 0.00
, 'return'
, ( select id from users where email = "igalaviz@allabordo.com" )
, 'T2104CORP14' );
SET @idTicketReturn = (SELECT last_insert_id());

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by)
values (5000.0 , 1 , 'Regreso de depósito en grarantía' , (SELECT @idTicketReturn) , 5000.0 ,  ( select id from users where email = "igalaviz@allabordo.com" ) );