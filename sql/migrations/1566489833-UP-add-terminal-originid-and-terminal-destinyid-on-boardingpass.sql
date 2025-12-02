-- 1566489833 UP add-terminal-originid-and terminal-destinyid-on-boardingpass
ALTER TABLE boarding_pass
ADD COLUMN terminal_origin_id INT(11) NOT NULL,
ADD COLUMN terminal_destiny_id INT(11) NOT NULL;
