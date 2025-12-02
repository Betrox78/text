-- 1567635737 DOWN last-ticket-cash-registers
ALTER TABLE cash_registers
DROP COLUMN last_ticket;

ALTER TABLE cash_out
DROP COLUMN last_ticket,
DROP COLUMN init_ticket;

ALTER TABLE boarding_pass_ticket
DROP COLUMN ticket_number;

ALTER TABLE boarding_pass_tracking
DROP COLUMN ticket_number;