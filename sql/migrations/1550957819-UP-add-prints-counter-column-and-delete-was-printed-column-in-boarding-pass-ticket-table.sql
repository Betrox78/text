-- 1550957819 UP add-prints-counter-column-and-delete-was-printed-column-in-boarding-pass-ticket-table
ALTER TABLE boarding_pass_ticket
DROP COLUMN was_printed,
ADD COLUMN prints_counter int(11) NOT NULL DEFAULT 0 COMMENT 'Prints counter' AFTER has_complements;