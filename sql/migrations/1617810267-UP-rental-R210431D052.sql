-- 1617810267 UP rental-R210431D052


UPDATE rental set payment_status = '1',   kilometers_init = 100 , kilometers_end = 110 , rent_status = 3 , penalty_days = 0 , received_at = DATE_ADD('2021-04-05 21:01:00', INTERVAL 1 HOUR) , received_by = (select id from users where email = "igalaviz@allabordo.com") , delivered_at = DATE_ADD('2021-04-04 21:01:00', INTERVAL 1 HOUR) , delivered_by =  (select id from users where email = "igalaviz@allabordo.com")  where reservation_code = "R210431D052";


-- INSERT CHECKLIST

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values ( (select id from rental where reservation_code = "R210431D052") , 10 , 6 , 0 , 0 , 6 , 0 , 0.0 , 0.0 , 1 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

-- INSERT RETURN DE GARANTIA

insert into tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code )
VALUES (
 (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1)
, (select id from rental where reservation_code = "R210431D052")
, 0
, 5000
, 5000
, 0.00
, 'return'
, ( select id from users where email = "igalaviz@allabordo.com" )
, 'T2104CORP15' );
SET @idTicketReturn = (SELECT last_insert_id());

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by)
values (5000.0 , 1 , 'Regreso de depósito en grarantía' , (SELECT @idTicketReturn) , 5000.0 ,  ( select id from users where email = "igalaviz@allabordo.com" ) );
