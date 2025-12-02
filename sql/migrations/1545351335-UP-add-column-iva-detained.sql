-- 1545351335 UP add-column-iva-detained
ALTER TABLE parcels
ADD COLUMN parcel_iva decimal(12,2) DEFAULT 0.0 AFTER iva;