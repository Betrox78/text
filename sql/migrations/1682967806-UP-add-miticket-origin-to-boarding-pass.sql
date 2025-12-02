-- 1682967806 UP add miticket-origin-to-boarding-pass
ALTER TABLE boarding_pass MODIFY COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer', 'miticket');