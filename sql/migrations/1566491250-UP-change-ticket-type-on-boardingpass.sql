-- 1566491250 UP change-ticket-type-on-boardingpass
ALTER TABLE boarding_pass
MODIFY ticket_type ENUM('abierto', 'abierto_sencillo','sencillo','redondo','abierto_redondo');

UPDATE boarding_pass SET ticket_type = 'abierto_sencillo' WHERE ticket_type = 'abierto';

ALTER TABLE boarding_pass
MODIFY ticket_type ENUM('abierto_sencillo','sencillo','redondo','abierto_redondo');