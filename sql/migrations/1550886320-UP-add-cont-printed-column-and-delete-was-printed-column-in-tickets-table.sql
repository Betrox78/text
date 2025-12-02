-- 1550886320 UP add-cont-printed-column-and-delete-was-printed-column-in-tickets-table
ALTER TABLE tickets
DROP COLUMN was_printed,
ADD COLUMN prints_counter int(11) NOT NULL DEFAULT 0 COMMENT 'Prints counter' AFTER action;