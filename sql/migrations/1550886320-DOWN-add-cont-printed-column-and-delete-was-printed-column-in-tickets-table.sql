-- 1550886320 DOWN add-cont-printed-column-and-delete-was-printed-column-in-tickets-table
ALTER TABLE tickets
DROP COLUMN prints_counter,
ADD COLUMN was_printed tinyint(1) NOT NULL DEFAULT 0 AFTER action;