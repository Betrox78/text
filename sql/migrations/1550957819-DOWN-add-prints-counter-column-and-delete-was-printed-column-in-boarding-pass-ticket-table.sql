-- 1550957819 DOWN add-prints-counter-column-and-delete-was-printed-column-in-boarding-pass-ticket-table
ALTER TABLE boarding_pass_ticket
DROP COLUMN prints_counter,
ADD COLUMN was_printed tinyint(1) NOT NULL DEFAULT 0 AFTER has_complements;