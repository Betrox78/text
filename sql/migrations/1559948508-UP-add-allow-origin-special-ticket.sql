-- 1559948508 UP add-allow-origin-special-ticket

ALTER TABLE special_ticket
ADD COLUMN origin_allowed varchar(100) DEFAULT 'sucursal,web,kiosko,app cliente,app chofer'  AFTER has_preferent_zone;