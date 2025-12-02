-- 1542938502 UP add-ticket-id-expenses

ALTER TABLE expense
ADD COLUMN ticket_id int(11) DEFAULT NULL AFTER payment_method_id;
ALTER TABLE expense ADD KEY fk_expense_ticket_id (ticket_id);
ALTER TABLE expense ADD CONSTRAINT fk_expense_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (id);