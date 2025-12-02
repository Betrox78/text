-- 1566489833 DOWN add-terminal-originid-and terminal-destinyid-on-boardingpass
ALTER TABLE boarding_pass
DROP COLUMN terminal_origin_id,
DROP COLUMN terminal_destiny_id;