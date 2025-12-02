-- 1567635737 UP last-ticket-cash-registers
ALTER TABLE cash_registers
ADD COLUMN last_ticket INT(11) DEFAULT 0 AFTER cash_register;

ALTER TABLE cash_out
ADD COLUMN last_ticket INT(11) DEFAULT 0 AFTER notes,
ADD COLUMN init_ticket INT(11) DEFAULT 0 AFTER notes;

ALTER TABLE boarding_pass_ticket
ADD COLUMN ticket_number INT(11) DEFAULT 0 AFTER tracking_code;

ALTER TABLE boarding_pass_tracking
ADD COLUMN ticket_number INT(11) DEFAULT 0 AFTER notes;