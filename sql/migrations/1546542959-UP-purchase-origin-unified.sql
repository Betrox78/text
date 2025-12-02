-- 1546542959 UP purchase-origin-unified
ALTER TABLE boarding_pass
CHANGE purchase_origin purchase_origin enum('sucursal','web','kiosko','app cliente','app chofer', 'plataforma') NOT NULL DEFAULT 'sucursal';

UPDATE boarding_pass SET purchase_origin = 'sucursal' WHERE purchase_origin = 'plataforma';

ALTER TABLE boarding_pass
CHANGE purchase_origin purchase_origin enum('sucursal','web','kiosko','app cliente','app chofer') NOT NULL DEFAULT 'sucursal';

ALTER TABLE rental
CHANGE purchase_origin purchase_origin enum('sucursal','web','kiosko','app cliente','app chofer') NOT NULL DEFAULT 'sucursal';

ALTER TABLE parcels
ADD COLUMN purchase_origin enum('sucursal','web','kiosko','app cliente','app chofer') NOT NULL DEFAULT 'sucursal';