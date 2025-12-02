-- 1729065842 DOWN parcels-purchase-origin-webservice
ALTER TABLE parcels MODIFY COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer');