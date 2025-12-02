-- 1543885849 UP add-fields-payback-in-boarding_pass-rental-and-parcels
ALTER TABLE boarding_pass
ADD COLUMN payback decimal(12,2) DEFAULT NULL COMMENT 'Monto generado por la compra';

ALTER TABLE parcels
ADD COLUMN payback decimal(12,2) DEFAULT NULL COMMENT 'Monto generado por la compra';

ALTER TABLE rental
ADD COLUMN payback decimal(12,2) DEFAULT NULL COMMENT 'Monto generado por la compra';
