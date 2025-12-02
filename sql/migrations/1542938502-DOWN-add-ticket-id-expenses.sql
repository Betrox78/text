-- 1542938502 DOWN add-ticket-id-expenses

ALTER TABLE expense
DROP COLUMN ticket_id;
ALTER TABLE expense DROP CONSTRAINT fk_expense_ticket_id;