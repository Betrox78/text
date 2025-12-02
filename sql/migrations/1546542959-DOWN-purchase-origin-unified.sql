-- 1546542959 DOWN purchase-origin-unified
-- BOARDING PASS
ALTER TABLE boarding_pass
CHANGE purchase_origin purchase_origin enum('plataforma','web','kiosko','app cliente','app chofer', 'sucursal') NULL DEFAULT 'plataforma';

UPDATE boarding_pass SET purchase_origin = 'plataforma' WHERE purchase_origin = 'sucursal';

ALTER TABLE boarding_pass
CHANGE purchase_origin purchase_origin enum('plataforma','web','kiosko','app cliente','app chofer') NULL DEFAULT 'plataforma';

-- RENTAL
ALTER TABLE rental
CHANGE purchase_origin purchase_origin enum('sucursal','web','kiosko','app cliente') NOT NULL DEFAULT 'sucursal';

-- PARCELS
ALTER TABLE parcels
DROP COLUMN purchase_origin;