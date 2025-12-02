-- 1717402573 UP tickets table parcel iva column
ALTER TABLE tickets
ADD COLUMN parcel_iva DECIMAL(12, 2) NOT NULL DEFAULT 0.0 AFTER iva;