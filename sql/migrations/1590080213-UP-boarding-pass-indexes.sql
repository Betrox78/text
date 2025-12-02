-- 1590080213 UP boarding-pass-indexes
CREATE INDEX boarding_pass_travel_return_date_idx ON boarding_pass(travel_return_date);
CREATE INDEX boarding_pass_ticket_type_idx ON boarding_pass(ticket_type);
CREATE INDEX boarding_pass_conekta_order_id_idx ON boarding_pass(conekta_order_id);
CREATE INDEX boarding_pass_reservation_code_idx ON boarding_pass(reservation_code);
CREATE INDEX boarding_pass_in_payment_idx ON boarding_pass(in_payment);
CREATE INDEX boarding_pass_is_phone_reservation_idx ON boarding_pass(is_phone_reservation);
