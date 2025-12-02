-- 1614442862 UP rental-R2102E06DDD-to-finish

-- RENTA R2102E06DDD

-- UPDATE RENTAL

UPDATE rental set   kilometers_init = 100 , kilometers_end = 110 , rent_status = 3 , penalty_days = 0 , received_at = "2021-02-19 11:36:00" , received_by = (select id from users where email = "igalaviz@allabordo.com") , delivered_at = "2021-02-19 11:37:00" , delivered_by =  (select id from users where email = "igalaviz@allabordo.com")  where reservation_code = "R2102E06DDD" ;


UPDATE rental set payment_status = '1' where reservation_code = "R2102E06DDD" ;

-- INSERT TICKETS

insert into tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code )
VALUES (
 (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1)
, (select id from rental where reservation_code = "R2102E06DDD")
, 220.14
, 6596
, 6596
, 0.00
, 'income'
, ( select id from users where email = "igalaviz@allabordo.com" )
, 'T2102CORP05' );
SET @idTicket = (SELECT last_insert_id());


-- INSERT TICKET DETALLES

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by)
values (5000.0 , 1 , 'Depósito en garantía' , (SELECT @idTicket) , 5000.0 ,  ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by)
values (1596.0 , 1 , 'Pago parcial renta' , (SELECT @idTicket) , 1596.0 ,  ( select id from users where email = "igalaviz@allabordo.com" ) );

-- INSERT PAGOS

insert into payment(amount,is_deposit,payment_method_id,currency_id,is_extra_charge,created_by,rental_id,ticket_id,payment_method)
values ( 5000 , true , 1 , 22 , false , ( select id from users where email = "igalaviz@allabordo.com" ) , (select id from rental where reservation_code = "R2102E06DDD") , (SELECT @idTicket) ,'cash' );
SET @idPaymentGarantia = (SELECT last_insert_id());

insert into payment(amount,is_deposit,payment_method_id,currency_id,is_extra_charge,created_by,rental_id,ticket_id,payment_method)
values ( 1596 , false , 1 , 22 , false , ( select id from users where email = "igalaviz@allabordo.com" ) , (select id from rental where reservation_code = "R2102E06DDD") , (SELECT @idTicket) ,'cash' );
SET @idPaymentAnticipo = (SELECT last_insert_id());

-- INSERT CASHOUT MOVE

INSERT INTO cash_out_move (cash_out_id, payment_id, quantity, move_type, created_by)
VALUES (
(select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1)
, (SELECT @idPaymentGarantia ), 5000.0 , '0' , ( select id from users where email = "igalaviz@allabordo.com" ) );

INSERT INTO cash_out_move (cash_out_id, payment_id, quantity, move_type, created_by)
VALUES (
(select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1)
, (SELECT @idPaymentAnticipo ), 1596.0 , '0' , ( select id from users where email = "igalaviz@allabordo.com" ) );

-- INSERT CHECKLIST

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values ( (select id from rental where reservation_code = "R2102E06DDD") , 10 , 6 , 0 , 0 , 6 , 0 , 0.0 , 0.0 , 1 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

-- INSERT RETURN DE GARANTIA

insert into tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code )
VALUES (
 (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1)
, (select id from rental where reservation_code = "R2102E06DDD")
, 0
, 5000
, 5000
, 0.00
, 'return'
, ( select id from users where email = "igalaviz@allabordo.com" )
, 'T2102CORP06' );
SET @idTicketReturn = (SELECT last_insert_id());

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by)
values (5000.0 , 1 , 'Regreso de depósito en grarantía' , (SELECT @idTicketReturn) , 5000.0 ,  ( select id from users where email = "igalaviz@allabordo.com" ) );