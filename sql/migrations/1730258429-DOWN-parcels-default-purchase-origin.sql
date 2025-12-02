-- 1730258429 DOWN parcels-default-purchase-origin
ALTER TABLE parcels MODIFY COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer') DEFAULT 'sucursal';