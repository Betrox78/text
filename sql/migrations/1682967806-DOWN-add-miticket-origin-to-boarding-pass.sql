-- 1682967806 DOWN add miticket-origin-to-boarding-pass
ALTER TABLE boarding_pass MODIFY COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer');