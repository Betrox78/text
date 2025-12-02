-- 1565976710 UP add-field-origin-of-the-sale-shipment
ALTER TABLE shipments
ADD COLUMN origin ENUM('web', 'app') NOT NULL DEFAULT 'app';