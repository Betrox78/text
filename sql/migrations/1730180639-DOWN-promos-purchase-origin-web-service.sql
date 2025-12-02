-- 1730180639 DOWN promos-purchase-origin-web-service
ALTER TABLE promos MODIFY COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer');