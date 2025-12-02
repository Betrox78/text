-- 1554850796 UP add column waybill in parcels table
ALTER TABLE parcels
ADD COLUMN waybill VARCHAR(60) DEFAULT NULL after parcel_tracking_code;