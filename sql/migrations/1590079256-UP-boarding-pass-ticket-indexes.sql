-- 1590079256 UP boarding-pass-ticket-indexes
CREATE INDEX boarding_pass_ticket_tracking_code_idx ON boarding_pass_ticket(tracking_code);
CREATE INDEX boarding_pass_ticket_ticket_status_idx ON boarding_pass_ticket(ticket_status);
CREATE INDEX boarding_pass_ticket_status_idx ON boarding_pass_ticket(status);
CREATE INDEX boarding_pass_ticket_checkedin_at_idx ON boarding_pass_ticket(checkedin_at);
CREATE INDEX boarding_pass_ticket_check_in_idx ON boarding_pass_ticket(check_in);
CREATE INDEX boarding_pass_ticket_seat_idx ON boarding_pass_ticket(seat);
CREATE INDEX boarding_pass_ticket_rfid_1_idx ON boarding_pass_ticket(rfid_1);
CREATE INDEX boarding_pass_ticket_rfid_2_idx ON boarding_pass_ticket(rfid_2);
CREATE INDEX boarding_pass_ticket_created_at_idx ON boarding_pass_ticket(created_at);


