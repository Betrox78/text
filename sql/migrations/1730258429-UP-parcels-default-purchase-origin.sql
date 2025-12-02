-- 1730258429 UP parcels-default-purchase-origin
ALTER TABLE parcels MODIFY COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer', 'web service') DEFAULT 'sucursal';