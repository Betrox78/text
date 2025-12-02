-- 1570492892 UP add column delivery time in parcels table
ALTER TABLE parcels
ADD COLUMN delivery_time INT(11) DEFAULT 24 AFTER promise_delivery_date;