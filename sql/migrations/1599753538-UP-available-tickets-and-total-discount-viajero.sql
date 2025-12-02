-- 1599753538 UP available-tickets-and-total-discount-viajero
update special_ticket set available_tickets = -1 , total_discount = 100 where name = 'Viajero Frecuente';

update config_ticket_price set discount = amount where special_ticket_id = (select id from special_ticket where name = 'Viajero Frecuente');

update  config_ticket_price set total_amount = (amount - discount) where special_ticket_id = (select id from special_ticket where name = 'Viajero Frecuente');