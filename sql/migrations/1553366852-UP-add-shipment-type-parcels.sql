-- 1553366852 UP add-shipment-type-parcels

ALTER TABLE parcels
ADD COLUMN shipment_type enum('OCU', 'EAD') NOT NULL DEFAULT 'OCU' AFTER delivery_time;