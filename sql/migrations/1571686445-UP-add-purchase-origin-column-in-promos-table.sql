-- 1571686445 UP add purchase origin column in promos table
ALTER TABLE promos
ADD COLUMN purchase_origin enum('sucursal','web','kiosko','app cliente','app chofer') DEFAULT NULL AFTER available_days;