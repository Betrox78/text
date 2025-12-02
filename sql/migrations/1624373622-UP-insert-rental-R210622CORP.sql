-- 1624373622 UP insert-rental-R210622CORP
insert into rental(init_route,init_full_address,farthest_route,farthest_full_address,total_passengers,vehicle_id,has_driver,amount,extra_charges,discount,driver_cost,total_amount,departure_date,departure_time,return_date,return_time,pickup_at_office,leave_at_office,pickup_branchoffice_id,leave_branchoffice_id,destiny_city_name,rental_price_id,guarantee_deposit,first_name,last_name,phone,email,address,reservation_code,updated_by,payment_status,created_by,quotation_expired_after,rental_minamount_percent,rental_price,branchoffice_id,pickup_city_id,leave_city_id,cost,profit , credential_type , no_credential , file_credential , rent_status , penalty_days , kilometers_init , created_at , kilometers_end , delivered_at ,  delivered_by , received_by , received_at)
values ('[24.789033,-107.442973]','BLVD. Miguel Tamayo, #S/N, Desarrollo Urbano 3 Ríos, Culiacán Rosales, Culiacán, Sinaloa, México, CP.80020','[24.789033,-107.442973]','BLVD. Miguel Tamayo, #S/N, Desarrollo Urbano 3 Ríos, Culiacán Rosales, Culiacán, Sinaloa, México, CP.80020',5,33,false,8788.00,0,0,0,8788.00,'2021-06-17','19:00','2021-06-21','19:00',true,true,5,5,'Saltillo',12,5000,'Atalo','Leal Sotelo','6672564799','atalolealsotelo@gmail.com','Tenochtitlan #355, Centro, 81200 Los Mochis, Sin.','R2106E2CORP', ( select id from users where email = "igalaviz@allabordo.com" ) ,'1', ( select id from users where email = "igalaviz@allabordo.com" ) ,15,10.0,2197.00,1,51,51,0.0,7575.86 , 'credential' , 'LPBLJS73071825H8000' , '69db386a-860d-8183-8a6c-af88ef157d7c.jpeg' , 3 , 0 , 100, '2021-05-31 21:54:39', 110 , '2021-06-21 19:01:01', ( select id from users where email = "igalaviz@allabordo.com" )  , ( select id from users where email = "igalaviz@allabordo.com" ) , '2021-06-17 19:01:01' );
SET @idRental = (SELECT last_insert_id());

INSERT INTO tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code , created_at ) VALUES ( (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1) , ( select @idRental ), 1212.14 , 8788.00 , 8788.00 , 0.0 , 'purchase' , ( select id from users where email = "igalaviz@allabordo.com" ) , 'T2106BCORP1', '2021-05-31 22:54:39');
SET @idTicket = (SELECT last_insert_id());

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by) values (8788.00, 1 , 'Anticipo renta' , ( SELECT @idTicket ) , 8788.00 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into payment(amount,is_deposit,payment_method_id,currency_id,is_extra_charge,created_by,rental_id,ticket_id,payment_method  , created_at ) values (8788,false,5,22,false, ( select id from users where email = "igalaviz@allabordo.com" ) ,( SELECT @idRental ),( SELECT @idTicket ),'cash',  '2021-05-31 22:54:39');
SELECT @idPayment = (SELECT last_insert_id());

INSERT INTO cash_out_move (cash_out_id, payment_id, quantity, move_type, created_by, created_at) VALUES ( (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1) , ( SELECT @idPayment ), 8788.00, '0', ( select id from users where email = "igalaviz@allabordo.com" ) ,  '2021-05-31 22:54:39');

-- Insert garantia

INSERT INTO tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code )
VALUES ( (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1) , ( select @idRental ), 0.0 , 5000.0 , 5000.0 , 0.0 , 'income' , ( select id from users where email = "igalaviz@allabordo.com" ) , 'T2106CORP22');
SET @idTicketG = (SELECT last_insert_id());

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by) values (5000.0, 1 , 'Depósito en garantía' , ( SELECT  @idTicketG ) , 5000.0 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into payment(amount,is_deposit,payment_method_id,currency_id,is_extra_charge,created_by,rental_id,ticket_id,payment_method)
values (5000,true,1,22,false,( select id from users where email = "igalaviz@allabordo.com" ),( SELECT @idRental ),( SELECT @idTicketG ),'cash');
SELECT @idPaymentG = (SELECT last_insert_id());

INSERT INTO cash_out_move (cash_out_id, payment_id, quantity, move_type, created_by)
VALUES (  (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1) , ( SELECT @idPaymentG ), 5000.0, '0', ( select id from users where email = "igalaviz@allabordo.com" ));


-- INSERT CHECKLIST


insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 7 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 6 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 5 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental) , 2 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 3 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 39 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 29 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 40 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 26 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 21 , 2 , 0 , 0 , 2 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 23 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 18 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 20 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental) , 13 , 4 , 0 , 0 , 4 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 24 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 25 , 2 , 0 , 0 , 2 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 16 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 17 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values ((SELECT @idRental ) , 37 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values ((SELECT @idRental) , 12 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental) , 38 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 35 , 2 , 0 , 0 , 2 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 9 , 2 , 0 , 0 , 2 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values ((SELECT @idRental ) , 14 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 11 , 4 , 0 , 0 , 4 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 15 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 22 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental) , 19 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 4 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 28 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 27 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental) , 8 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 1 , 1 , 0 , 0 , 1 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

insert into rental_checklist ( rental_id , checklist_van_id , delivery_quantity , unit_price , amount  , reception_quantity , damage_quantity , damage_percent , damage_amount , status_on_reception , status , created_by  )
values (( SELECT @idRental ) , 10 , 6 , 0 , 0 , 6 , 0 , 0.0 , 0.0 , 0 , 1 , ( select id from users where email = "igalaviz@allabordo.com" ) );

-- INSERT DRIVER

INSERT INTO rental_driver ( rental_id , first_name , last_name , no_licence , expired_at , file_licence , created_by)
VALUES ( ( select @idRental ) , 'Leal Sotelo' , 'Atalo' , 'LPBLJS73071825H8000' , '2023-02-22' , '69db386a-860d-8183-8a6c-af88ef157d7c.jpeg' , ( select id from users where email = "igalaviz@allabordo.com" ) );


-- INSERT RETURN DE GARANTIA

insert into tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code )
VALUES (
 (select id from cash_out where employee_id = ( select id from employee where user_id = (select id from users where email = "igalaviz@allabordo.com")) and cash_out_status = 1)
, ( SELECT @idRental )
, 0
, 5000
, 5000
, 0.00
, 'return'
, ( select id from users where email = "igalaviz@allabordo.com" )
, 'T2106CORP23' );
SET @idTicketReturn = (SELECT last_insert_id());

insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by)
values (5000.0 , 1 , 'Regreso de depósito en grarantía' , (SELECT @idTicketReturn) , 5000.0 ,  ( select id from users where email = "igalaviz@allabordo.com" ) );
