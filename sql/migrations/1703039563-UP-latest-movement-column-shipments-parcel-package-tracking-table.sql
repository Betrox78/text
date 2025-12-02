-- 1703039563 UP latest movement column shipments parcel package tracking table
ALTER TABLE shipments_parcel_package_tracking
ADD COLUMN latest_movement BOOLEAN DEFAULT FALSE AFTER trailer_id;