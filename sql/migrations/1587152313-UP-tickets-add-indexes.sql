-- 1587152313 UP tickets-add-indexes
CREATE INDEX parcels_ticket_code_idx ON tickets(ticket_code);
CREATE INDEX parcels_action_idx ON tickets(action);
CREATE INDEX parcels_status_idx ON tickets(status);
CREATE INDEX parcels_created_at_idx ON tickets(created_at);